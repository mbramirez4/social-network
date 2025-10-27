package socialnetwork.Service;

import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.Edge;
import org.graphstream.graph.implementations.SingleGraph;

import socialnetwork.Model.Profile;

public class ConnectionsVisualizer {
    private static ProfilesManager profilesManager;

    public static void setProfilesManager(ProfilesManager profilesManager) {
        ConnectionsVisualizer.profilesManager = profilesManager;
    }
    
    private ProfileTree tree;

    public ConnectionsVisualizer() {
        tree = new ProfileTree(profilesManager.getProfilesIDs());
    }

    public void drawGraph(UUID profileID, int maxFriendshipLevel) {
        computeTree(profileID, maxFriendshipLevel);
        tree.drawTree();
    }

    private void computeTree(UUID profileID, int maxFriendshipLevel) {
        Profile tempProfile;
        Set<UUID> addedFriends = new HashSet<>();
        
        Set<UUID> currentLevelProfiles = new HashSet<>();
        currentLevelProfiles.add(profileID);
        
        for (int i = 0; i < maxFriendshipLevel; i++) {
            for (UUID id : currentLevelProfiles) {
                if (tree.getCount() <= 1) {
                    break;
                }

                tempProfile = profilesManager.getProfile(id);
                addedFriends.addAll(connectFriends(tempProfile));
            }
            currentLevelProfiles = Set.copyOf(addedFriends);
            addedFriends.clear();
        }
    }

    private Set<UUID> connectFriends(Profile profile) {
        HashSet<UUID> connectedProfiles = new HashSet<>();
        Map<UUID, Integer> friends = profile.getFriends();

        boolean connected = false;
        int connectionStrength = 0;
        UUID parentID = profile.getProfileID();
        for (Map.Entry<UUID, Integer> friend : friends.entrySet()) {
            UUID friendIndex = friend.getKey();
            connectionStrength = friend.getValue();

            connected = tree.union(parentID, friendIndex, connectionStrength);
            if (connected) {
                connectedProfiles.add(friendIndex);
            }
        }

        return connectedProfiles;
    }

    private class ProfileTree {
        /* connectionsStrength[i] is the strength of the connection
         * between the profile at index i and its parent */
        private int[] connectionsStrength;

        /* parent[i] is the parent of the profile at index i */
        private int[] parent;

        private Map<UUID, Integer> profileToIndex = new HashMap<>();

        private int count;

        // creates a tree for the given profile
        public ProfileTree(UUID[] ids) {
            count = ids.length;
            parent = new int[ids.length];
            connectionsStrength = new int[ids.length];

            int selfConnectionStrength = 5; // strength of the connection between a profile and itself
            
            int index = 0;
            for (UUID id : ids) {
                profileToIndex.put(id, index);
                parent[index] = index;
                connectionsStrength[index] = selfConnectionStrength;
                index++;
            }
        }

         /**
         * Returns the canonical element of the set containing element {@code p}.
         *
         * @param  profileID id of an element
         * @return the canonical element of the set containing {@code p}
         * @throws IllegalArgumentException unless {@code 0 <= p < n}
         */
        public int find(UUID profileID) {
            Integer p = profileToIndex.get(profileID);
            if (p == null) {
                throw new IllegalArgumentException("Profile with id " + profileID + " does not exist");
            }

            while (p != parent[p])
                p = parent[p];
            return p;
        }

        /**
         * Returns true if the two elements are in the same set.
         *
         * @param  p one element
         * @param  q the other element
         * @return {@code true} if {@code p} and {@code q} are in the same set;
         *         {@code false} otherwise
         * @throws IllegalArgumentException unless
         *         both {@code 0 <= p < n} and {@code 0 <= q < n}
         */
        public boolean connected(UUID p, UUID q) {
            return find(p) == find(q);
        }

        /**
         * Merges the set containing element {@code p} with the set
         * containing element {@code q} if they are in different sets
         * and if {@code q} is a root element of its own tree. In that
         * case, {@code p} is set as the parent of {@code q}.
         *
         * @param  p one element
         * @param  q the other element
         * @return {@code true} if the union is successful; {@code false}
         *         otherwise
         * @throws IllegalArgumentException unless
         *         both {@code 0 <= p < n} and {@code 0 <= q < n}
         */
        public boolean union(UUID p, UUID q, int connectionStrength) {
            int rootP = find(p);
            int rootQ = find(q);
            if (rootP == rootQ) return false;

            int qIndex = profileToIndex.get(q);
            if (rootQ != qIndex) {
                throw new IllegalArgumentException("Profile with id " + q + " is not a root element. Connecting it would damage it's current tree structure");
            }

            parent[qIndex] = profileToIndex.get(p);
            connectionsStrength[qIndex] = connectionStrength;
            count--;
            
            return true;
        }

        public int getCount() {
            return count;
        }

        public void drawTree(){
            Node node = null;
            Edge edge = null;

            Graph graph = new SingleGraph("Friendships cluster");
            graph.setAttribute("ui.stylesheet",
                    "node { fill-color: lightblue; size: 25px; text-size: 15px; } "
                            + "edge { fill-color: gray; size: 2px; }");

            Set<Map.Entry<UUID, Integer>> entries = profileToIndex.entrySet();
            for (Map.Entry<UUID, Integer> entry : entries) {
                node = graph.addNode(String.valueOf(entry.getValue()));
                node.setAttribute("ui.label", profilesManager.getProfile(entry.getKey()).getName());
                node.setAttribute("ui.profileID", entry.getKey().toString());
            }

            graph.display();

            int i = -1;
            for (Map.Entry<UUID, Integer> entry : entries){
                i = entry.getValue();
                if (parent[i] != i){
                    edge = graph.addEdge(i + "-" + parent[i], String.valueOf(i), String.valueOf(parent[i]));
                    edge.addAttribute("ui.strength", connectionsStrength[i]);
                }
            }

        }
    }
}
