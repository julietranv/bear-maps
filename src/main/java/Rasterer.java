import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {

    public Rasterer() {

    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        System.out.println(params);
        Map<String, Object> results = new HashMap<>();
        MapServer server = new MapServer();
        double lrlon = params.get("lrlon");
        double ullon = params.get("ullon");
        double lrlat = params.get("lrlat");
        double ullat = params.get("ullat");
        double width = params.get("w");
        double height = params.get("h");
        double lonDPP = (lrlon - ullon) / width;
        boolean success = false;
        int depth = 7;
        double xInterval = (server.ROOT_LRLON - server.ROOT_ULLON) / Math.pow(2, 7);
        for (int d = 0; d <= 7; d++) {
            double step = (server.ROOT_LRLON - server.ROOT_ULLON) / Math.pow(2, d);
            double tempDPP = step / server.TILE_SIZE;
            if (tempDPP <= lonDPP) {
                depth = d;
                xInterval = step;
                break;
            }
        }
        int k = (int) Math.pow(2, depth);
        int minX = 0;
        int maxX = 0;
        double yInterval = (server.ROOT_ULLAT - server.ROOT_LRLAT) / k;
        int minY = 0;
        int maxY = 0;
        boolean xDone = false;
        boolean yDone = false;
        for (int i = 0; i < k; i++) {
            if (server.ROOT_ULLON + (i * xInterval) <= ullon) {
                minX = i;
            }
            if (server.ROOT_ULLON + ((i + 1) * xInterval) >= lrlon && !xDone) {
                maxX = i;
                xDone = true;
            }
            if (server.ROOT_ULLAT - (i * yInterval) >= ullat) {
                minY = i;
            }
            if (server.ROOT_ULLAT - ((i + 1) * yInterval) <= lrlat && !yDone) {
                maxY = i;
                yDone = true;
            }
            if (xDone && yDone) {
                break;
            }
        }
        String[][] grid = new String[maxY - minY + 1][maxX - minX + 1];
        for (int i = 0; i < grid.length; i++) {
            for (int j = 0; j < grid[0].length; j++) {
                grid[i][j] = "d" + Integer.toString(depth)
                        + "_x" + Integer.toString(minX + j)
                        + "_y" + Integer.toString(minY + i)
                        + ".png";
            }
        }
        success = true;
        results.put("render_grid", grid);
        results.put("raster_ul_lon", server.ROOT_ULLON + (minX * xInterval));
        results.put("raster_lr_lon", server.ROOT_ULLON + ((maxX + 1) * xInterval));
        results.put("raster_ul_lat", server.ROOT_ULLAT - (minY * yInterval));
        results.put("raster_lr_lat", server.ROOT_ULLAT - ((maxY + 1) * yInterval));
        results.put("depth", depth);
        results.put("query_success", success);

        System.out.println("Since you haven't implemented getMapRaster, nothing is displayed in "
                           + "your browser.");
        return results;
    }
}
