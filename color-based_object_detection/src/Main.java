import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class Main {

    /* GLOBAL VARIABLES */
    static int WIDTH = 640;
    static int HEIGHT = 480;
    static final double threshold = 1.0;
    static BufferedImage inputImg;
    static List<BufferedImage> objImg = new ArrayList<>();
    static List<String> objNames = new ArrayList<>();
    static List<DetectedObject> detectedObjects = new ArrayList<>();



    /* Class Objects */
    private static final RGBImage RGBImage = new RGBImage();
    private static final Histogram histogram = new Histogram();
    private static final BoundingBox boundingbox = new BoundingBox();


    static class DetectedObject {
        Rectangle boundingBox;
        String name;

        DetectedObject(Rectangle boundingBox, String name) {
            this.boundingBox = boundingBox;
            this.name = name;
        }
    }

    private static void createAndCompareHistogram() {
        int[][] inputImgHistogram = histogram.getHistogram(inputImg, "input");
        for (BufferedImage obj : objImg) {
            processObjectImage(obj, inputImgHistogram);
        }
    }
    private static void processObjectImage(BufferedImage currentObjectImg, int[][] inputImgHistogram) {

        int[][] objImgHistogram = histogram.getHistogram(currentObjectImg, "object");
        double similarityRes = histogram.compareNormalizedHistograms(inputImgHistogram, objImgHistogram);

        if (similarityRes > threshold) {
            List<Rectangle> boxes = boundingbox.identifyAndAddBoundingBoxes(inputImg, inputImgHistogram, objImgHistogram);

            for (Rectangle box : boxes) {
                String objName = objNames.get(objImg.indexOf(currentObjectImg));
                detectedObjects.add(new DetectedObject(box, objName));
            }
        }
    }

    private static void outputResults() {
        Graphics2D g = inputImg.createGraphics();
        g.setColor(Color.BLUE);

        Font font = new Font("Arial", Font.BOLD, 13);
        g.setFont(font);

        for (DetectedObject detected : detectedObjects) {
            Rectangle box = detected.boundingBox;
            g.drawRect(box.x, box.y, box.width, box.height);

            int textXPosition = box.x + 30;
            int textYPosition = box.y + box.height - 20;
            g.drawString(detected.name, textXPosition, textYPosition);
        }

        g.dispose();
        RGBImage.showImg(inputImg);
    }

    public static void commandLineParsing(String[] args){
        String inputPath = args[0];
        List<String> objPaths = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
        System.out.println("INPUT FILE: " + "\n" + inputPath.substring(inputPath.lastIndexOf('/') + 1));
        System.out.println();
        System.out.println("OBJECT FILE(s):");
        for (String path : objPaths){
            System.out.println(path.substring(path.lastIndexOf('/') + 1));
            objNames.add(path.substring(path.lastIndexOf('/') + 1));
        }


        // Computing BufferedImages for InputImg and ObjImgs
        inputImg = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        RGBImage.readImageRGB(WIDTH, HEIGHT, inputPath, inputImg);
        for (String path : objPaths) {
            BufferedImage obj_img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            RGBImage.readImageRGB(WIDTH, HEIGHT, path, obj_img);
            objImg.add(obj_img);
        }
    }


    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Insufficient Command Line Arguments");
            return;
        }
        commandLineParsing(args);
        System.out.println("\n" + "PROGRESS: Creating and Comparing Histograms (this may take awhile)");
        createAndCompareHistogram();
        System.out.println("PROGRESS: Results in Output Image");
        outputResults();
    }
}
