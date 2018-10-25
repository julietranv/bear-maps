import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    private HashMap<Long, Node> verticesHash = new HashMap<>();
    private HashMap<Long, Edge> edgesHash = new HashMap<>();
    private HashMap<Long, Node> trashHash = new HashMap<>();
    private Trie locations = new Trie();

    public static class Node {
        // implements Comparable<Node>
        private long id;
        private double lon;
        private double lat;
        private String location = null;
        private ArrayList<Node> adjacent = new ArrayList<>();


        public Node(long id, double lon, double lat) {
            this.id = id;
            this.lon = lon;
            this.lat = lat;
        }

        public void setLoc(String loc) {
            location = loc;
        }
    }

    public static class Edge {
        String speedMax;
        Node first;
        Node second;
        long id;

        public Edge(long id, String max, Node n1, Node n2) {
            this.id = id;
            first = n1;
            second = n2;
            speedMax = max;
        }
    }

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
        System.out.println(locations.root.actualName);
    }

    public void addNode(Node n) {
        verticesHash.put(n.id, n);
    }

    public void addEdge(Edge e) {
        edgesHash.put(e.id, e);
        e.first.adjacent.add(e.second);
        e.second.adjacent.add(e.first);
    }

    public Node getNode(long v) {
        return verticesHash.get(v);
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        ArrayList<Node> toClean = new ArrayList<>();
        for (Node n : verticesHash.values()) {
            if (n.adjacent.isEmpty()) {
                toClean.add(n);
            }
            if (n.location != null) {
                locations.insert(n.location, n.id);
                System.out.println("Location added to tree: " + n.location);
            }
        }
        for (Node node : toClean) {
            verticesHash.remove(node.id, node);
            trashHash.put(node.id, node);
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        ArrayList<Long> vertices = new ArrayList<>();
        for (Node n : verticesHash.values()) {
            vertices.add(n.id);
        }
        return vertices;
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        Node node = verticesHash.get(v);
        ArrayList<Long> ids = new ArrayList<>();
        for (Node n : node.adjacent) {
            ids.add(n.id);
        }
        return ids;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        long close = 0;
        double minDist = Double.MAX_VALUE;
        double phi2 = Math.toRadians(lat);
        for (Node n : verticesHash.values()) {
            double phi1 = Math.toRadians(n.lat);
            double dphi = Math.toRadians(lat - n.lat);
            double dlambda = Math.toRadians(lon - n.lon);
            double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
            a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0)
                    * Math.sin(dlambda / 2.0);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double dist = 3963 * c;
            if (dist < minDist) {
                minDist = dist;
                close = n.id;
            }
        }
        return close;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        Node node = verticesHash.get(v);
        return node.lon;
    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        Node node = verticesHash.get(v);
        return node.lat;
    }


    /**
     * In linear time, collect all the names of OSM locations that prefix-match the query string.
     * @param prefix Prefix string to be searched for. Could be any case, with our without
     *               punctuation.
     * @return A <code>List</code> of the full names of locations whose cleaned name matches the
     * cleaned <code>prefix</code>.
     */
    public List<String> getLocationsByPrefix(String prefix) {
        return locations.getLocationsByPrefix(prefix);
    }

    /**
     * Collect all locations that match a cleaned <code>locationName</code>, and return
     * information about each node that matches.
     * @param locationName A full name of a location searched for.
     * @return A list of locations whose cleaned name matches the
     * cleaned <code>locationName</code>, and each location is a map of parameters for the Json
     * response as specified: <br>
     * "lat" : Number, The latitude of the node. <br>
     * "lon" : Number, The longitude of the node. <br>
     * "name" : String, The actual name of the node. <br>
     * "id" : Number, The id of the node. <br>
     */
    public List<Map<String, Object>> getLocations(String locationName) {
        return locations.getLocations(locationName);
    }

    private class Trie {
        TrieNode root;

        public Trie() {
            root = new TrieNode();
        }

        public void insert(String word, long id) {
            TrieNode curr = root;
            String cleaned = cleanString(word);
            if (cleaned.length() < 1) {
                curr.actualName.add(word);
                curr.id.add(id);
                return;
            }
            String first = cleaned.substring(0, 1);
            if (curr.isLeaf() || !curr.hasChild(first)) {
                curr.addChild(first, new TrieNode(first));
            }
            curr = curr.getChild(first);
            if (cleaned.length() == 1) {
                curr.actualName.add(word);
                curr.id.add(id);
                return;
            }
            insert(cleaned.substring(1), first, word, id, curr);
        }

        public void insert(String word, String soFar, String original, long id, TrieNode parent) {
            TrieNode curr = parent;
            String first = word.substring(0, 1);
            if (curr.isLeaf() || !curr.hasChild(soFar + first)) {
                curr.addChild(soFar + first, new TrieNode(first));
            }
            curr = curr.getChild(soFar + first);
            if (word.length() == 1) {
                curr.actualName.add(original);
                curr.id.add(id);
                return;
            }
            insert(word.substring(1), soFar + first, original, id, curr);
        }

        public List<String> getLocationsByPrefix(String prefix) {
            ArrayList<String> matches = new ArrayList<>();
            TrieNode pointer = root;
            prefix = cleanString(prefix);
            for (int i = 1; i <= prefix.length(); i++) {
                String sub = prefix.substring(0, i);
                if (pointer.hasChild(sub)) {
                    pointer = pointer.getChild(sub);
                } else {
                    return matches;
                }
            }
            getWords(pointer, matches);
            return matches;
        }

        public void getWords(TrieNode curr, ArrayList<String> m) {
            if (curr.isEnd()) {
                for (String name : curr.actualName) {
                    m.add(name);
                }
            }
            if (!curr.isLeaf()) {
                for (TrieNode node : curr.children.values()) {
                    getWords(node, m);
                }
            }
        }

        public List<Map<String, Object>> getLocations(String locationName) {
            List<Map<String, Object>> allInfo = new ArrayList<>();
            locationName = cleanString(locationName);
            TrieNode pointer = root;
            if (locationName.length() >= 1) {
                for (int i = 1; i <= locationName.length(); i++) {
                    String sub = locationName.substring(0, i);
                    if (pointer.hasChild(sub)) {
                        pointer = pointer.getChild(sub);
                    } else {
                        return allInfo;
                    }
                }
            }
            if (pointer.isEnd()) {
                for (int i = 0; i < pointer.actualName.size(); i++) {
                    Map<String, Object> info = new HashMap<>();
                    long id = pointer.id.get(i);
                    if (verticesHash.containsKey(id)) {
                        info.put("lat", verticesHash.get(id).lat);
                        info.put("lon", verticesHash.get(id).lon);
                        info.put("name", pointer.actualName.get(i));
                        info.put("id", id);
                        allInfo.add(info);
                    } else if (trashHash.containsKey(id)) {
                        info.put("lat", trashHash.get(id).lat);
                        info.put("lon", trashHash.get(id).lon);
                        info.put("name", pointer.actualName.get(i));
                        info.put("id", id);
                        allInfo.add(info);
                    }
                }
            }
            return allInfo;
        }
    }

    private class TrieNode {
        public String label;
        public HashMap<String, TrieNode> children = new HashMap<>();
        public ArrayList<String> actualName = new ArrayList<>();
        public ArrayList<Long> id = new ArrayList<>();

        public TrieNode() {
            label = null;
        }

        public TrieNode(String c) {
            label = c;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }

        public void addChild(String word, TrieNode t) {
            children.put(word, t);
        }

        public boolean hasChild(String word) {
            return children.containsKey(word);
        }

        public TrieNode getChild(String word) {
            return children.get(word);
        }

        public boolean isEnd() {
            return (!actualName.isEmpty());
        }
    }
}
