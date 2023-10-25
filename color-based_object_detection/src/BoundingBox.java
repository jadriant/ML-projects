import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Stack;
import java.util.List;
import java.util.Arrays;

public class BoundingBox {

    int WIDTH = 640;
    int HEIGHT = 480;
    double threshold = 5; // 5 works except apple for oswald
    public ColorSpaceConverter csc = new ColorSpaceConverter();
    public RGBImage RGBimage = new RGBImage();

    class Point {
        int x, y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    List<Rectangle> identifyAndAddBoundingBoxes(BufferedImage inputImg, int[][] inputImgHistogram, int[][] objImgHistogram) {
        BufferedImage probImg = objectDetection(inputImg, inputImgHistogram, objImgHistogram);
        BufferedImage resImg = postProcessing(probImg);
//        RGBimage.showImg(resImg);
        return new ArrayList<>(getDetectedObjects(resImg));
    }

    /*
        BACK PROJECTION
        * Works by modeling the color distribution of a target image and then projecting this distribution onto another image.
        * It effectively answers the question, "How likely is each pixel in this image to belong to the color distribution of my target?"
     */
    public BufferedImage objectDetection(BufferedImage img, int[][] inputImgHistogram, int[][] objImgHistogram) {
        BufferedImage probImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Color clr = new Color(img.getRGB(x, y));
                double ratio = computeRatio(clr, inputImgHistogram, objImgHistogram);
                probImg.setRGB(x, y, convertRatioToGrayColor(ratio));
            }
        }
        return probImg;
    }

    // helper functions for backProjectionMethod()
    private double computeRatio(Color clr, int[][] inputImgHistogram, int[][] objImgHistogram) {
        int[] yuvValues;
        int u, v;
        yuvValues = csc.RGBtoYUV(clr.getRed(), clr.getGreen(), clr.getBlue());
        u = yuvValues[1];
        v = yuvValues[2];
        double uChannelRatio = (objImgHistogram[0][u] + 1.0) / (inputImgHistogram[0][u] + 1.0);
        double vChannelRatio = (objImgHistogram[1][v] + 1.0) / (inputImgHistogram[1][v] + 1.0);
        return (uChannelRatio + vChannelRatio) / 2.0;
    }
    private int convertRatioToGrayColor(double ratio) {
        int grayValue = (int) (255 * ratio);
        grayValue = Math.min(255, Math.max(0, grayValue));
        return new Color(grayValue, grayValue, grayValue).getRGB();
    }

    /*
        POST-PROCESSING AFTER BACK-PROJECTION
        - Thresholding: convert grayscale probabilityImage to a binary image
        - Erosion: morphological operation that "shrinks" white regions and enlarges black ones
        - Dilation: morphological operation that "expands" white regions and shrinks black ones
     */
    private static final int KERNEL_SIZE = 50;
    private static final int COLOR_WHITE = Color.WHITE.getRGB();
    private static final int COLOR_BLACK = Color.BLACK.getRGB();

    BufferedImage postProcessing(BufferedImage probImg) {
        BufferedImage thresholdImg = applyThreshold(probImg);
        BufferedImage erodedImg = applyErode(thresholdImg);
        return applyDilate(erodedImg);
    }

    private BufferedImage applyThreshold(BufferedImage img) {
        BufferedImage thresholdImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Point p = new Point(x, y);
                int intensity_score = new Color(img.getRGB(p.x, p.y)).getRed();
                thresholdImg.setRGB(p.x, p.y, (intensity_score >= threshold) ? COLOR_WHITE : COLOR_BLACK);
            }
        }
        return thresholdImg;
    }

    private BufferedImage processImg(BufferedImage source, boolean isErosion) {
        BufferedImage resultImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        for (int x = KERNEL_SIZE; x < WIDTH - KERNEL_SIZE; x++) {
            for (int y = KERNEL_SIZE; y < HEIGHT - KERNEL_SIZE; y++) {
                Point p = new Point(x, y);
                boolean shouldChange = checkSurroundingPixels(source, p, isErosion);
                resultImage.setRGB(p.x, p.y, shouldChange ? COLOR_WHITE : COLOR_BLACK);
            }
        }
        return resultImage;
    }

    private boolean checkSurroundingPixels(BufferedImage img, Point p, boolean isErosion) {
        Color targetColor = isErosion ? Color.BLACK : Color.WHITE;

        for (int i = -KERNEL_SIZE; i <= KERNEL_SIZE; i++) {
            for (int j = -KERNEL_SIZE; j <= KERNEL_SIZE; j++) {
                Point nearby = new Point(p.x + i, p.y + j);
                if (new Color(img.getRGB(nearby.x, nearby.y)).equals(targetColor)) {
                    return !isErosion;
                }
            }
        }
        return isErosion;
    }

    private BufferedImage applyErode(BufferedImage img) {
        return processImg(img, true);
    }

    private BufferedImage applyDilate(BufferedImage img) {
        return processImg(img, false);
    }


    /*
        FIND BOUNDING BOXES using DFS (connected components labeling)
        - scan the img and cluster together adjacent white pixels
        - condition: intensity greater than threshold
     */
    List<Rectangle> getDetectedObjects(BufferedImage img) {
        List<Rectangle> detectedObjects = new ArrayList<>();
        boolean[][] markedPixels = new boolean[WIDTH][HEIGHT];

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Point p = new Point(x, y);
                if (isValidStartingPixel(p, img, markedPixels)) {
                    Rectangle box = exploreBox(p, img, markedPixels);
                    if (box != null) {
                        detectedObjects.add(box);
                    }
                }
            }
        }
        return detectedObjects;
    }

    private boolean isValidStartingPixel(Point p, BufferedImage img, boolean[][] markedPixels) {
        return !markedPixels[p.x][p.y] && new Color(img.getRGB(p.x, p.y)).getRed() > threshold;
    }

    private Rectangle exploreBox(Point start, BufferedImage img, boolean[][] markedPixels) {
        int leftBound = start.x;
        int rightBound = start.x;
        int upperBound = start.y;
        int lowerBound = start.y;

        Stack<Point> pixels = new Stack<>();
        pixels.push(start);
        while (!pixels.isEmpty()) {
            Point position = pixels.pop();
            if (!isValidPixel(position, img, markedPixels)) continue;

            markedPixels[position.x][position.y] = true;
            leftBound = Math.min(position.x, leftBound);
            rightBound = Math.max(position.x, rightBound);
            upperBound = Math.min(position.y, upperBound);
            lowerBound = Math.max(position.y, lowerBound);

            pixels.addAll(neighboringPixels(position));
        }

        if (leftBound == rightBound || upperBound == lowerBound) return null;
        return new Rectangle(leftBound, upperBound, rightBound - leftBound, lowerBound - upperBound);
    }

    // Helpers for exploreBox()
    private List<Point> neighboringPixels(Point p) {
        return Arrays.asList(
                new Point(p.x - 1, p.y),
                new Point(p.x + 1, p.y),
                new Point(p.x, p.y - 1),
                new Point(p.x, p.y + 1)
        );
    }

    private boolean isValidPixel(Point p, BufferedImage img, boolean[][] visited) {
        return p.x >= 0 && p.x < WIDTH && p.y >= 0 && p.y < HEIGHT && !visited[p.x][p.y] && new Color(img.getRGB(p.x, p.y)).getRed() > threshold;
    }

}
