package org.firstinspires.ftc.teamcode;
import java.util.ArrayList;
import org.opencv.core.*;
import org.openftc.easyopencv.OpenCvPipeline;
import org.openftc.easyopencv.OpenCvCamera;
import org.opencv.imgproc.Imgproc;


public class SampleOrientation extends OpenCvPipeline {
    //need to implement public MatprocessFrame (Mat input)
    //this function is called for every new camera fram
    //within processFrame() you use OpenCV functions on the input image
    //the Mat Object is a matrix representing the image pixels

    //we are going to change the RGB matrix to HSV matrix
    /*HSV = Hue(type of color), Saturation(intensity/purity of color),
    and value(brightness) */
    private Mat matHSV = new Mat();

    //going to filter a matrix for only red
    private Mat maskRed = new Mat();


    //going to filter a matrix for only Blue
    private Mat maskBlue = new Mat();


    //going to filter a matrix for only Yellow
    private Mat maskYellow = new Mat();


    //going to filter a matrix for all colors
    private Mat maskCombined = new Mat();


    //now we create a class for each individual block
    //we can utilize this class to differentiate between the
    //different blocks in the submersible or on field (alliance specific vs shared) etc.
    public static class DetectedBlock{
        public String color;
        public double angle;
        public double distance;
        public SamplePosition position;

    }


    //any image or Mat you return from processFrame() will be shown on the driver hub.


    //in auto after you initialize camera you set it to use the pipeline
    //the pipeline runs in the background processing continuously
    //OpMode periodically (only when prompted to) can read data from the pipeline
    //-> using info from pipeline the robot can then make decisions


    /*enum is a type of data that enables
    a variable to be a set of predefined constants*/
    public enum SamplePosition {
        LEFT,CENTER,RIGHT,NOT_FOUND
    }

    //creating a public variable that stores objects of type DetectedBlock
    //volatile marking ensures its shared between threads
    /*volatile basically means that the variable
    might be acessed by multiple threads, so the system
    should always read the latest version of it from memory

    We are sharing the detectedBlocks variable between program loops
    (sharing pipeline between auto and teleop loops)
    * */
    //starts off empty
    public volatile ArrayList<DetectedBlock> detectedBlocks = new ArrayList<>();


    //following method is called when pipeline is started
    //changes from RGB matrix to HSV matrix
    @Override
    public void init(Mat firstFrame){
        Imgproc.cvtColor(firstFrame, matHSV, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(matHSV, matHSV, Imgproc.COLOR_RGB2HSV);
    }

    /*
    * Imgproc.cvtColor() is an opencv method used to convert
    * an image from one color space to another
    * Imgproc.cvtColor(Mat source, Mat destination, int conversionCode);
    * source is the source image
    * destination mat object is where the converted image is stored
    * conversionCode is a constant telling OpenCV what type of conversion to do
    *
    * */


    @Override
    public Mat processFrame(Mat input){
        //step 1: Color space conversion
        //EasyOpenCV gives us RGBA input so we need to convter to RGB then to HSV
        //this is for easier color filtering

        Imgproc.cvtColor(input, matHSV, Imgproc.COLOR_RGBA2RGB);
        Imgproc.cvtColor(matHSV, matHSV, Imgproc.COLOR_RGB2HSV);

        //step 2: creating a threshold by color to create masks

        //We are going to make some scalars
        /*
        * scalars in java are used to represent colors or pixel values
        *
        * Scalar color = new Scalar(a, b, c, d); //the fourth value is optional
        *
        * for RGB the scalar is:
        * Scalar(Blue, Green, Red)
        *
        * for HSV the scalar is:
        * Scalar(Hue, Saturation, Value)
        *
        * Right now we are working with HSV color space so we use the following:
        * */


        //We defined a color range
        //hue: 0-> 10
        //Saturation: 100 -> 255
        //Value: 100 -> 255

        //Scalar for Red
        Scalar lowRed1 = new Scalar(0,100,100);
        Scalar highRed1 = new Scalar(10,255,255);

        Scalar lowRed2 = new Scalar(160, 100, 100);
        Scalar highRed2 = new Scalar(180, 255, 255);

        //Scalar for Blue
        Scalar lowBlue  = new Scalar(100, 150, 70);
        Scalar highBlue  = new Scalar(140, 255, 255);

        //scalar for Yellow
        Scalar lowYellow= new Scalar(20, 150, 70);
        Scalar highYellow= new Scalar(40, 255, 255);

        //step 2.5: creating a mask

        /*
        * Masks in OpenCV act as a filter
        * By using the scalar it creates a filter of black and white pixels where the white
        * are red and the black is everything else
        *
        * That is what Core.inRange() function does
        *
        * Core.inRange(inputMat, lowerBound, upperBound, outputMask);
        *
        * inputMask: that is the HSV image we will analyze
        * lowerBound: this is the scalar that defines the minimum color
        * upperBound: This is the scalar that defines the maximum color
        * outputMask: This is the Mat where OpenCV will store the result
        * */

        // Mask for blue
        Core.inRange(matHSV, lowBlue, highBlue, maskBlue);
        // Mask for yellow
        Core.inRange(matHSV, lowYellow, highYellow, maskYellow);


        //the mask for red is a little different
        /*
        * The HSV Hue value wraps like a Circle
        * For red it starts around 0 and also picks up again around 180
        * you need two ranges to capture the full red hue spectrum
        *
        * What is basically happening is:
        * Make a mask of the first red range
        * Make a mask of the second red range
        * Now combine them using a bitwise OR operation -> red = either one
        * */


        Core.inRange(matHSV, lowRed1, highRed1, maskRed);
        Mat maskRed2 = new Mat();
        Core.inRange(matHSV, lowRed2, highRed2, maskRed2);
        Core.bitwise_or(maskRed, maskRed2, maskRed);
        maskRed2.release();

        /*
        * Core.bitwise_or()
        * This combines two binary images pixel by pixel
        * Core.bitwise_or(Mask A, Mask B, result Mask)
        *
        * maskRed2.release() is used to manually free native memory used by mat object
        * **had to do some trouble shooting for this
        * basically mats are not normal java objects so they store a lot of memory
        * we don't need maskRed2 after combining the red mask using the bitwise function
        * if we didn't release maskRed2 a new copy of the mask will be created for every frame
        * this will overload the memory
        *
        * use .release() when creating a new matrix inside the processFrame() loop or any loop for that matter
        * */


        //now we can combine all the Yellow, blue, and red masks into one so all 3 colors are detected

        Core.bitwise_or(maskRed, maskBlue, maskCombined);
        Core.bitwise_or(maskCombined, maskYellow, maskCombined);



        //Step 3: Finding contours of masks
        /*
        * Contours refers to a line or shape that defines the outline or boundary of something
        * These are lines on a map that connects points of equal elevation showing the shape of
        * the land surface
        *
        * its used for depth perception
        * */


        /*MatOfPoint is a subclass of superclass Mat which is
        * made to hold a list of 2D points representing contours
        *
        * for the contours list we made, each MatOfPoint is
        * one detected shape which contains a list of point objects
        * that trade the shapes edge
        * essentially contours list we made is a list of a list of points
        *  */
        ArrayList<MatOfPoint> contours = new ArrayList<>();

        Mat hierarchy = new Mat();


        /*
        * Imgproc.findContours()
        * is a function that finds the outlines of objects
        *
        * Imgproc.findContours(Mat image, ArrayList<MatOfPoint> contourList, Mat hierarchy, int mode, int method);
        *
        * Mat image = the image we create contours out of
        * ArrayList<MatOfPoint> contourList = place we store contours
        * Mat Hierarchy = a place to store nesting relationships between contours
        * -> EX. if a shape has a hole in it, the outer shape is one contour and the hole is another
        * -> the hiearchy tells OpenCV which contours are parents, children or siblings
        * -> we are going to throw away the children contours by releasing the Hiearchy Mat
        *
        * int mode = determines which contours to return
        * -> OpenCV has many ways to group contours
        * -> common values are .RETR_EXTERNAL (outermost contours) -> no holes
        * -> .RETR_TREE (all contours and hierarchy)
        * -> .RETR_LIST (all contours but no hiearchy)
        *
        * In other games we would use RETR_TREE or RETR_LIST if we are detecting holes
        * for Into the deep we don't have to worry about holes
        *
        * int method = How to simplify the contour
        * -> .CHAIN_APPROX_SIMPLE tells OpenCV to approximate contours to reduce the number of points
        * -> .CHAIN_APPROX_NONE stores all edge points
        *
        * */
        Imgproc.findContours(maskCombined, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


        //now remember to release all mats created in the loop to save memory
        hierarchy.release();


        //Step 4 finding block information



        //Explain??????
        detectedBlocks.clear();

        //Explain??????
        //Getting the bound rectangle
        //probably have to refer back to the githubs of other teams for this


        //Explain??????
        //Finding the Center


        //Explain??????
        //Calculating the Angle using the bound rectangle
        //probably have to refer back to the githubs of other teams for this












    }

}
