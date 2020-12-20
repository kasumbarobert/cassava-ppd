
package cassava.ppd;



import fiji.util.gui.OverlayedImageCanvas;
import graphcut.GraphCut;
import graphcut.Terminal;
import ij.ImagePlus;

import ij.Prefs;
import ij.gui.ImageWindow;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.io.Opener;
import ij.plugin.Thresholder;
import ij.plugin.tool.OverlayBrushTool;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import inra.ijpb.binary.BinaryImages;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.io.File;
import static java.lang.Double.NaN;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;



import org.scijava.command.Command;
import org.scijava.plugin.Plugin;
import java.awt.Point;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.imagej.ops.Ops;




/*
 * @author robertkasumba
 */


@Plugin(type = Command.class, menuPath = "Plugins>Cassava PPD")
public class PPPComputerPlugin implements Command {
	private JButton loadImgButton;
	private JButton segmentCassavaButton;
	private JButton resetButton;
	private JButton setBackgroundColorButton;
        private JButton dilateButton;
        private DoubleJSlider sigmaSlider;
	private JLabel sigmaText;
	private final ExecutorService exec = Executors.newFixedThreadPool(1);

        private double sigma =0.5;
        
        private static final int OBJ_OVERLAY_MISSING =1;
        private static final int BKG_OVERLAY_MISSING =1;
	
    private static ImagePlus imp,impCopy;
   private static ImageJ ij;
   private  PPDWindow customWindow;



   
    public static JFrame f;
    public static boolean state = false;

    
    public PPPComputerPlugin() {
        this.listener = new ActionListener() {
            public void actionPerformed(final ActionEvent e) {

                exec.submit(new Runnable() {
                    public void run()
                    {
                        if(e.getSource() == loadImgButton)
                        {
                            try{
                                loadImage();
                            }catch(Exception e){
                                e.printStackTrace();
                            }
                        }
                        else if(e.getSource() == segmentCassavaButton){
                             segmentGraphCut2(imp);  
                        }
                        else if(e.getSource() == setBackgroundColorButton){
                          
                            Color color =Toolbar.getForegroundColor();
                            if(color == Color.GREEN){
                                color =Color.RED;
                            }
                            else if(color == Color.RED){
                                color =Color.GREEN;
                            }
                            else{
                                 color =Color.GREEN;
                            }
                            Toolbar.setForegroundColor(color);

//                            segmentGraphCut2(imp); 
                            
                        }
                         else if(e.getSource() == dilateButton){
                          
                         try{
                      

                              ImagePlus dup = binarize(imp);
                              dup.show();

                              ImageProcessor p =  dup.getProcessor();
                              p.dilate();

                         
                            
                         }catch(Exception e){
                         }
                            
                        }
                        else if(e.getSource() == resetButton){
                  
                            imp = impCopy;
                            impCopy = imp.duplicate(); //back up the image and reset
                            customWindow.setImage(imp);
                        }
                        
                        
                        
                    }
                });
            }
        };
    }
    
    public void init(){
		loadImgButton = new JButton("Load Image");
		loadImgButton.setToolTipText("Load Cassava Image");
		
		segmentCassavaButton = new JButton("Segment Cassava");
		segmentCassavaButton.setToolTipText("Compute PPD");		
		segmentCassavaButton.setEnabled(true);
                
		resetButton = new JButton("Reset");
		resetButton.setToolTipText("Reset");		
		resetButton.setEnabled(true);
                
                setBackgroundColorButton= new JButton("Toggle Brush Color");
                setBackgroundColorButton.setToolTipText("Mark Cassava");		
                setBackgroundColorButton.setEnabled(true);
                Toolbar.setBrushSize(10);
                Toolbar.setForegroundColor(Color.GREEN);
                // create a slider 
                sigmaSlider = new DoubleJSlider(1, 100, 10,100); 
        
                // set spacing  

                dilateButton = new JButton("Dilate");
		dilateButton.setToolTipText("Dilate");		
		dilateButton.setEnabled(true);
                
                
                sigmaSlider.setPaintTrack(true); 
                sigmaSlider.setPaintTicks(true); 
                sigmaSlider.setPaintLabels(true); 
                
                sigmaSlider.addChangeListener(new ChangeListener(){
                    public void stateChanged(ChangeEvent e) 
                    { 
                        sigmaText.setText("Sigma: " + sigmaSlider.getScaledValue()); 
                        sigma = sigmaSlider.getScaledValue();
                    } 
                });
                sigmaText  = new JLabel(); 
                sigmaText.setText("Sigma: " + sigmaSlider.getScaledValue()); 
    }

   
        
   @Override
    public void run() {
    loadImage();
    init();
    customWindow= new PPDWindow(imp);
    }
//    

    /**
     * This main function serves for development purposes.
     * It allows you to run the plugin immediately out of
     * your integrated development environment (IDE).
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        ij = new ImageJ();
        ij.ui().showUI();
//
        ij.command().run(PPPComputerPlugin.class, true);

    }
    
   
        public void loadImage(){
         // ask the user for a file to open
        final File file = ij.ui().chooseFile(null, "open");
        

        if (file != null) {
            // load the dataset
            Opener opener = new Opener();
            imp = opener.openImage(file.getAbsolutePath());
            impCopy = imp.duplicate();
            if(customWindow != null){
                customWindow.setImage(imp);
            }
        }
    }
        
        public void segmentGraphCut2(ImagePlus imagePlus){
                ij.gui. Overlay overlay = imagePlus.getOverlay();
                int pixels[] = (int [])imagePlus.getProcessor().getPixels();
                System.out.println("Here 2...");
                GraphCut graphCut;
                if(overlay.size()>=2){
                    try{
                        ArrayList <Roi> roisBkg = new ArrayList<>();
                        ArrayList <Roi> roisObj = new ArrayList<>();
                        Color objColor =overlay.get(0).getStrokeColor();

                        for (int i=0; i<overlay.size(); i++) {

                            if (overlay.get(i).getStrokeColor().equals(objColor)){ //marked object
                                roisObj.add(overlay.get(i));
                            }
                            else{
                                roisBkg.add(overlay.get(i));
                            }
                             
                        }
               
                    graphCut =generateGraph(imagePlus,roisBkg, roisObj);
                    double end, start=0;
                    end = System.currentTimeMillis();
                    // calculate max flow
                    System.out.println("Calculating max flow...");
                     start = System.currentTimeMillis();
                    float maxFlow = graphCut.computeMaximumFlow(true, null);
                    end = System.currentTimeMillis();
                    System.out.println("...done. Max flow is " + maxFlow + ". (" + (end - start) + "ms)");
                    for(int i=0; i<pixels.length; i++){
                        if(graphCut.getTerminal(i) != Terminal.FOREGROUND){
                             pixels[i]=0;
                        }
                    }
                    imagePlus.getProcessor().setPixels(getConnectedImage(imagePlus,overlay.get(0).getBounds().x,overlay.get(0).getBounds().y).getProcessor().getPixels());
//                   getConnectedImage(imagePlus,overlay.get(0).getBounds().x,overlay.get(0).getBounds().y).show();
                    overlay.clear();
                    }catch(OverlayMissingException e){
                        if(e.getMissingOverlay() == OBJ_OVERLAY_MISSING){
                             overlay.clear();
                        }
                        else{
                             overlay.remove(1);
                        }
                        ij.ui().show(e.getMessage());
                    }
                }else{
                    System.out.println(" Size : "+overlay.size());
                }
                System.out.println("Done...");
                
                imagePlus.updateAndDraw();



        }
    
        public void segmentGraphCut(ImagePlus imagePlus){
                ij.gui. Overlay overlay = imagePlus.getOverlay();
                int pixels[] = (int [])imagePlus.getProcessor().getPixels();
                System.out.println("Here...");
                GraphCut graphCut;
                if(overlay.size()>=2){
                    try{
                    graphCut =generateGraphBack(imagePlus,overlay.get(1), overlay.get(0));
                    double end, start=0;
                    end = System.currentTimeMillis();
                    // calculate max flow
                    System.out.println("Calculating max flow...");
                     start = System.currentTimeMillis();
                    float maxFlow = graphCut.computeMaximumFlow(true, null);
                    end = System.currentTimeMillis();
                    System.out.println("...done. Max flow is " + maxFlow + ". (" + (end - start) + "ms)");
                    for(int i=0; i<pixels.length; i++){
                        if(graphCut.getTerminal(i) != Terminal.FOREGROUND){
                             pixels[i]=0;
                        }
                    }
                    imagePlus.getProcessor().setPixels(getConnectedImage(imagePlus,overlay.get(0).getBounds().x,overlay.get(0).getBounds().y).getProcessor().getPixels());
//                   getConnectedImage(imagePlus,overlay.get(0).getBounds().x,overlay.get(0).getBounds().y).show();
                    overlay.clear();
                    }catch(OverlayMissingException e){
                        if(e.getMissingOverlay() == OBJ_OVERLAY_MISSING){
                             overlay.clear();
                        }
                        else{
                             overlay.remove(1);
                        }
                        ij.ui().show(e.getMessage());
                    }
                }else{
                    System.out.println(" Size : "+overlay.size());
                }
                System.out.println("Done...");
                
                imagePlus.updateAndDraw();



        }
        
        private class OverlayMissingException extends Exception{
            int missingOverlay;
    
            OverlayMissingException(int missing){
                missingOverlay = missing;
            }
            @Override
            public String getMessage(){
               if(missingOverlay == OBJ_OVERLAY_MISSING){
                   return "Reset and Mark again";
               }
               else{
                   return "Mark the Background again";
               }
            }
            public int getMissingOverlay(){
                return missingOverlay;
            }
        }
        
        public ImagePlus getConnectedImage(ImagePlus inputImg , int x, int y){
            HashMap<String, Boolean> visited = new HashMap<>();
            ImagePlus cpInputImg = inputImg.duplicate();
            int neighbors[][] = {{-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}};
            FloatProcessor fp = cpInputImg.getProcessor().convertToFloatProcessor();
            float[] pixels = (float[])fp.getPixels();
            Rectangle r = fp.getRoi();
            Stack <Point> stack = new Stack();
            stack.push(new Point(x,y));
            ImagePlus segmentedImage = new ImagePlus("Connected",inputImg.getProcessor().createProcessor(inputImg.getWidth(), inputImg.getHeight()));
            int pixelsSegmented [] = (int[])segmentedImage.getProcessor().getPixels();
            int pixelsInput [] = (int[])inputImg.getProcessor().getPixels();
            while(!stack.isEmpty()){
               Point p = stack.pop();
               visited.put(p.x+""+p.y, true);
               if(fp.getPixelValue(p.x, p.y)>0){
                    for(int n=0; n< 8; n++){
                        int ngbrY =neighbors[n][0]+p.y;
                        int ngbrX =neighbors[n][1]+p.x;
                        if((ngbrY<r.height && ngbrY>=0) && (ngbrX<r.width && ngbrX>=0)){        
                              if(fp.getPixelValue(ngbrX, ngbrY)>0 && !visited.containsKey(ngbrX+""+ngbrY) ){
                                  stack.push(new Point(ngbrX,ngbrY));
                              }
                        }
                       
                    }
                pixelsSegmented[p.y*r.width+p.x] = pixelsInput[p.y*r.width+p.x];
                
               }
           }
           
           return segmentedImage; 
        }
        
 
        public GraphCut generateGraph(ImagePlus inputImg, ArrayList<Roi> bkgRois, ArrayList<Roi> objRois) throws OverlayMissingException{
            //merge ROI
//             System.out.println("Here 1 segment..."+bkgRois.length+" Obj "+objRois.size());
            int graphdimens = inputImg.getProcessor().getPixelCount();//the two nodes sink and background are added
            
            int neighbors[][] = {{-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}};
            ImagePlus impBkg, impObj;
            System.out.println("Here 2 segment..."+bkgRois.size()+" Obj "+objRois.size());
            impBkg = new ImagePlus("Background",inputImg.getProcessor().createProcessor(inputImg.getWidth(), inputImg.getHeight()));
            impObj = new ImagePlus("Objet",inputImg.getProcessor().createProcessor(inputImg.getWidth(), inputImg.getHeight()));
            System.out.println("Rois");
            //create images
           Point minBkgPt = new Point(inputImg.getWidth(),inputImg.getHeight());
           Point maxBkgPt = new Point(0,0);
           Rectangle rBkg = new Rectangle(inputImg.getWidth()-1,inputImg.getHeight()-1,1,1);
//           System.out.println("Here 3 segment..."+bkgRois.length+" Obj "+objRois.length);
           for(Roi roi:bkgRois ){
                impBkg.getProcessor().insert(roi.getMask(),(int) roi.getBounds().getX(), (int)roi.getBounds().getY());
                rBkg.add(roi.getBounds());
           }
           Rectangle rObj = new Rectangle(inputImg.getWidth()-1,inputImg.getHeight()-1,1,1);
           for(Roi roi:objRois ){
                impObj.getProcessor().insert(roi.getMask(),(int) roi.getBounds().getX(), (int)roi.getBounds().getY());
                rObj.add(roi.getBounds());
           }
      
//           
           impBkg.setRoi(rBkg);
           impObj.setRoi(rObj);
//           impObj.show();
//           impBkg.show();
           
         
           
           return generateGraph(inputImg,impBkg,impObj);
           

            
        }
        public ImagePlus binarize(ImagePlus inputImg){
            ImagePlus cpInputImg = inputImg.duplicate();
            FloatProcessor fp = cpInputImg.getProcessor().convertToFloatProcessor();
            float[] pixels = (float[])fp.getPixels();
            int x,y, offset,i;
            Rectangle r = inputImg.getProcessor().getRoi();
            for ( y=r.y; y<(r.y+r.height); y++) {
                offset = y*r.width;
                for ( x=r.x; x<(r.x+r.width); x++) {
                    i = offset + x;
                    if(pixels[i]>0){
//                        System.out.println(i+" "+ pixels[i]);
                        pixels[i]=255;
                     }else{
                         pixels[i]=0;
                    }
              
                }
            }
           
            ImagePlus p =new ImagePlus("test",fp.convertToFloat());
//            p.getProcessor().convertToRGB();
            return p;
        }
        
        public  GraphCut generateGraph(ImagePlus inputImg, ImagePlus impBkg, ImagePlus impObj) throws OverlayMissingException{

            int graphdimens = inputImg.getProcessor().getPixelCount();//the two nodes sink and background are added

            int neighbors[][] = {{-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}};
            FloatProcessor objfProcessor = impObj.getProcessor().convertToFloatProcessor();
            FloatProcessor bkgfProcessor = impBkg.getProcessor().convertToFloatProcessor();
            float objPixels[] = (float [])objfProcessor.getPixels();
            float bkgPixels[] = (float [])bkgfProcessor.getPixels();
            int y =0;
            int  x =0;
            int offset, i;
            ImagePlus cpInputImg = inputImg.duplicate();
           
            FloatProcessor fp = cpInputImg.getProcessor().convertToFloatProcessor();
            float[] pixels = (float[])fp.getPixels();
     
            int histBkg[] = new int[256];
            int histObj[] = new int[256]; 
            for(i =0; i<256; i++){
                histBkg[i]=0;
                histObj[i]=0;
            }
            int countBkgPixels=0;
            int countObjPixels=0;
            ArrayList <Integer> objPosList = new ArrayList<>();
            ArrayList <Integer> bkgPosList = new ArrayList<>();
            Rectangle r = inputImg.getProcessor().getRoi();
            for ( y=r.y; y<(r.y+r.height); y++) {
                offset = y*r.width;
                for ( x=r.x; x<(r.x+r.width); x++) {
                    i = offset + x;
                if(bkgPixels[i]==255){
                     bkgPixels[i]= pixels[i];
                     histBkg[(int)pixels[i]]++;
                     countBkgPixels++;
                 }

                if(objPixels[i]==255){
                     objPixels[i]= pixels[i];
                     histObj[(int)pixels[i]]++;
                     countObjPixels++;
                 }

                }
            }
            if(countObjPixels==0){
                throw new OverlayMissingException(OBJ_OVERLAY_MISSING);
              
            }
            else if(countBkgPixels==0 ){
                 throw new OverlayMissingException(BKG_OVERLAY_MISSING);
            }
         
//            Rectangle rBkg =bkgRoi.getBounds();
//            Rectangle rObj = objRoi.getBounds();
//            objfProcessor.setRoi(rObj);
//            bkgfProcessor.setRoi(rBkg);
  
            
            r = inputImg.getProcessor().getRoi();
            int pos;
            int ngbrY, ngbrX, ngbrPos;
            float k=0, sumBpq;
            double  bpq =0;
            GraphCut graphCut = new GraphCut(graphdimens, 2*(graphdimens)+8*(graphdimens));

            double end, start=0;
            start = System.currentTimeMillis();
            for ( y=r.y; y<(r.y+r.height); y++) {
                offset = y*r.width;
                for ( x=r.x; x<(r.x+r.width); x++)  {
                    pos = offset + x;
                    float pixel = pixels[pos];
                    sumBpq=0;
                    for(int n=0; n< 8; n++){
                        ngbrY =neighbors[n][0]+y;
                        ngbrX =neighbors[n][1]+x;
                        if((ngbrY<r.height && ngbrY>=0) && (ngbrX<r.width && ngbrX>=0)){
                                ngbrPos = ngbrY*r.width+ngbrX;
                                float pixelNgbr = pixels[ngbrPos];
                                bpq = computeEdgeLikelihood(pixel,pixelNgbr,neighbors[n]);                                  
                                //Get the maximum bpq 
                                graphCut.setEdgeWeight(ngbrPos, pos, (float)bpq);
                              sumBpq+= (float)bpq;
                        }
                    }
                       
                    
                    if (sumBpq>k){
                        k =sumBpq;
                    }
                    //determine if this node is in background
                    if(objPixels[pos]!=0){ //this node is part of the object
                          objPosList.add(pos);
                         
                    }
                    else  if(bkgPixels[pos]!=0){ //this node is part of the background
                          bkgPosList.add(pos);
                    }
                    else { //the node is not part of any of object or background
                        graphCut.setTerminalWeights(pos,(float) computeNegativeLikelihood(histObj,countObjPixels,(int)pixel),(float)computeNegativeLikelihood(histBkg,countBkgPixels,(int)pixel));
                    }

                }

            }
             end = System.currentTimeMillis();
            System.out.println("...Generating Graph  (" + (end - start) + "ms)");
            k = k+1;
            System.out.println(k+" ");
            for(int objPos: objPosList){
                graphCut.setTerminalWeights(objPos,  k,0);
            }
            for(int bkgPos: bkgPosList){
                graphCut.setTerminalWeights(bkgPos, 0,k);
            }

            return graphCut;
        }

         public  GraphCut generateGraphBack(ImagePlus inputImg, Roi bkgRoi, Roi objRoi) throws OverlayMissingException{

            int graphdimens = inputImg.getProcessor().getPixelCount();//the two nodes sink and background are added

            int neighbors[][] = {{-1, -1}, {-1, 0}, {-1, 1}, {0, 1}, {1, 1}, {1, 0}, {1, -1}, {0, -1}};
            ImagePlus impBkg, impObj;
        
                impBkg = new ImagePlus("Background",inputImg.getProcessor().createProcessor(inputImg.getWidth(), inputImg.getHeight()));
            impObj = new ImagePlus("Objet",inputImg.getProcessor().createProcessor(inputImg.getWidth(), inputImg.getHeight()));
                impBkg.getProcessor().insert(bkgRoi.getMask(),(int) bkgRoi.getBounds().getX(), (int)bkgRoi.getBounds().getY());
                impObj.getProcessor().insert(objRoi.getMask(),(int) objRoi.getBounds().getX(), (int)objRoi.getBounds().getY());
           
            FloatProcessor objfProcessor = impObj.getProcessor().convertToFloatProcessor();
            FloatProcessor bkgfProcessor = impBkg.getProcessor().convertToFloatProcessor();
            float objPixels[] = (float [])objfProcessor.getPixels();
            float bkgPixels[] = (float [])bkgfProcessor.getPixels();
            int y =0;
            int  x =0;
            int offset, i;
            ImagePlus cpInputImg = inputImg.duplicate();
           
            FloatProcessor fp = cpInputImg.getProcessor().convertToFloatProcessor();
            float[] pixels = (float[])fp.getPixels();
     
            int histBkg[] = new int[256];
            int histObj[] = new int[256]; 
            for(i =0; i<256; i++){
                histBkg[i]=0;
                histObj[i]=0;
            }
            int countBkgPixels=0;
            int countObjPixels=0;
            ArrayList <Integer> objPosList = new ArrayList<>();
            ArrayList <Integer> bkgPosList = new ArrayList<>();
            Rectangle r = inputImg.getProcessor().getRoi();
            for ( y=r.y; y<(r.y+r.height); y++) {
                offset = y*r.width;
                for ( x=r.x; x<(r.x+r.width); x++) {
                    i = offset + x;
                if(bkgPixels[i]==255){
                     bkgPixels[i]= pixels[i];
                     histBkg[(int)pixels[i]]++;
                     countBkgPixels++;
                 }

                if(objPixels[i]==255){
                     objPixels[i]= pixels[i];
                     histObj[(int)pixels[i]]++;
                     countObjPixels++;
                 }

                }
            }
            if(countObjPixels==0){
                throw new OverlayMissingException(OBJ_OVERLAY_MISSING);
              
            }
            else if(countBkgPixels==0 ){
                 throw new OverlayMissingException(BKG_OVERLAY_MISSING);
            }
         
            Rectangle rBkg =bkgRoi.getBounds();
            Rectangle rObj = objRoi.getBounds();
            objfProcessor.setRoi(rObj);
            bkgfProcessor.setRoi(rBkg);
  
            
            r = inputImg.getProcessor().getRoi();
            int pos;
            int ngbrY, ngbrX, ngbrPos;
            float k=0, sumBpq;
            double  bpq =0;
            GraphCut graphCut = new GraphCut(graphdimens, 2*(graphdimens)+8*(graphdimens));
            
            for ( y=r.y; y<(r.y+r.height); y++) {
                offset = y*r.width;
                for ( x=r.x; x<(r.x+r.width); x++)  {
                    pos = offset + x;
                    float pixel = pixels[pos];
                    sumBpq=0;
                    for(int n=0; n< 8; n++){
                        ngbrY =neighbors[n][0]+y;
                        ngbrX =neighbors[n][1]+x;
                        if((ngbrY<r.height && ngbrY>=0) && (ngbrX<r.width && ngbrX>=0)){        
                              ngbrPos = ngbrY*r.width+ngbrX;
                              float pixelNgbr = pixels[ngbrPos];
                              bpq = computeEdgeLikelihood(pixel,pixelNgbr,neighbors[n]);                                  
                              //Get the maximum bpq 
                              sumBpq+= (float)bpq;
                              graphCut.setEdgeWeight(ngbrPos, pos, (float)bpq);
                        }
                        else{
                        }

                    }
                    if (sumBpq>k){
                        k =sumBpq;
                    }
                    //determine if this node is in background
                    if(objPixels[pos]!=0){ //this node is part of the object
                          objPosList.add(pos);
                         
                    }
                    else  if(bkgPixels[pos]!=0){ //this node is part of the background
                          bkgPosList.add(pos);
                    }
                    else { //the node is not part of any of object or background
                        graphCut.setTerminalWeights(pos,(float) computeNegativeLikelihood(histObj,countObjPixels,(int)pixel),(float)computeNegativeLikelihood(histBkg,countBkgPixels,(int)pixel));
                    }

                }

            }
            k = k+1;
            System.out.println(k+" ");
            for(int objPos: objPosList){
                graphCut.setTerminalWeights(objPos,  k,0);
            }
            for(int bkgPos: bkgPosList){
                graphCut.setTerminalWeights(bkgPos, 0,k);
            }

            return graphCut;
        }
        public float computeEdgeLikelihood(float pixel, float pixelNgbr, int pos[]){
            float intesityDiff = pixel-pixelNgbr;
            float distance = (float)Math.sqrt((pos[0]*pos[0])+(pos[1]*pos[1]));
            float bpq =(float) Math.exp(-1*(intesityDiff*intesityDiff/2*sigma*sigma))/distance;
           
            return bpq;
        }
        public double computeNegativeLikelihood(int hist[],int numPixels, int pixelValue){
            //Compute the likelihood i.e -ln(Prob(pixel/hist))
            if(hist[pixelValue]==0){
                return 0.0;
            }
            double likelihood =-Math.log(hist[pixelValue]/(double)numPixels);
//            System.out.println(pixelValue+" , pixels: "+numPixels+" "+likelihood);
            
            return likelihood;
        }

    /**
     * Listeners
     */
	private ActionListener listener;

	
    
    /**
	 * Custom canvas to deal with zooming an panning
	 */
	private class CustomCanvas extends OverlayedImageCanvas 
	{
            	private static final String WIDTH_KEY = "obrush.width";
                private float width = (float)Prefs.get(WIDTH_KEY, 5);
                private BasicStroke stroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND);
                private GeneralPath path;
                private boolean newPath;
               	private final int transparency=0;
		CustomCanvas(ImagePlus imp) 
		{
			super(imp);
			Dimension dim = new Dimension(Math.max(512, imp.getWidth()), Math.max(512, imp.getHeight()));
			setMinimumSize(dim);
			setSize(dim.width, dim.height);
			setDstDimensions(dim.width, dim.height);
			addKeyListener(new KeyAdapter() {
				public void keyReleased(KeyEvent ke) {
					repaint();
				}
			});
		}
                
                @Override
		public void setDrawingSize(int w, int h) {}

		public void setDstDimensions(int width, int height) {
			super.dstWidth = width;
			super.dstHeight = height;
			// adjust srcRect: can it grow/shrink?
			int w = Math.min((int)(width  / magnification), imp.getWidth());
			int h = Math.min((int)(height / magnification), imp.getHeight());
			int x = srcRect.x;
			if (x + w > imp.getWidth()) x = w - imp.getWidth();
			int y = srcRect.y;
			if (y + h > imp.getHeight()) y = h - imp.getHeight();
			srcRect.setRect(x, y, w, h);
			repaint();
		}

		//@Override
		public void paint(Graphics g) {
			Rectangle srcRect = getSrcRect();
			double mag = getMagnification();
			int dw = (int)(srcRect.width * mag);
			int dh = (int)(srcRect.height * mag);
			g.setClip(0, 0, dw, dh);

			super.paint(g);

			int w = getWidth();
			int h = getHeight();
			g.setClip(0, 0, w, h);

			// Paint away the outside
			g.setColor(getBackground());
			g.fillRect(dw, 0, w - dw, h);
			g.fillRect(0, dh, w, h - dh);
		}
	}
   
	private class PPDWindow extends ImageWindow 
	{
		/** layout for annotation panel */
		private GridBagLayout boxAnnotation = new GridBagLayout();
		/** constraints for annotation panel */
		private GridBagConstraints constraints = new GridBagConstraints();
		/** Panel with class radio buttons and lists */
		private final JPanel annotationsPanel = new JPanel();
		private final JPanel buttonsPanel = new JPanel();

		private final JPanel controlJPanel = new JPanel();
		private final JPanel optionsJPanel = new JPanel();

		private final Panel all = new Panel();
		final CustomCanvas canvas;
         
                ImagePlus loadeImp;
                Rectangle r;
                 
                public PPDWindow(ImagePlus imp) 
		{
                        
			super(imp, new CustomCanvas(imp));
                        this.loadeImp =imp;

			canvas = (CustomCanvas) getCanvas();
			
			removeAll();

			setTitle("PPD Calculator");
			
			// Annotations panel
			constraints.anchor = GridBagConstraints.NORTHWEST;
			constraints.gridwidth = 1;
			constraints.gridheight = 1;
			constraints.gridx = 0;
			constraints.gridy = 0;
			
			annotationsPanel.setBorder(BorderFactory.createTitledBorder("Labels"));
			annotationsPanel.setLayout(boxAnnotation);
			

//			// Add listeners
		
                        loadImgButton.addActionListener(listener);
                        segmentCassavaButton.addActionListener(listener);
                        resetButton.addActionListener(listener);
                        dilateButton.addActionListener(listener);
                        setBackgroundColorButton.addActionListener(listener);
			
			
			controlJPanel.setBorder(BorderFactory.createTitledBorder("Initialize"));
			GridBagLayout trainingLayout = new GridBagLayout();
			GridBagConstraints ctrlPanelConstraints = new GridBagConstraints();
			ctrlPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			ctrlPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			ctrlPanelConstraints.gridwidth = 1;
			ctrlPanelConstraints.gridheight = 1;
			ctrlPanelConstraints.gridx = 0;
			ctrlPanelConstraints.gridy = 0;
			ctrlPanelConstraints.insets = new Insets(5, 5, 6, 6);
			controlJPanel.setLayout(trainingLayout);
			
			controlJPanel.add(loadImgButton, ctrlPanelConstraints);
			ctrlPanelConstraints.gridy++;
			controlJPanel.add(segmentCassavaButton, ctrlPanelConstraints);
			ctrlPanelConstraints.gridy++;
                        controlJPanel.add(setBackgroundColorButton, ctrlPanelConstraints);
			ctrlPanelConstraints.gridy++;
                        controlJPanel.add(dilateButton, ctrlPanelConstraints);
                        ctrlPanelConstraints.gridy++;
                        controlJPanel.add(resetButton, ctrlPanelConstraints);
                        ctrlPanelConstraints.gridy++;
                        
			
                         optionsJPanel.setBorder(BorderFactory.createTitledBorder("Sigma"));
			GridBagLayout optionsLayout = new GridBagLayout();
			GridBagConstraints optionsConstraints = new GridBagConstraints();
			optionsConstraints.anchor = GridBagConstraints.NORTHWEST;
			optionsConstraints.fill = GridBagConstraints.HORIZONTAL;
			optionsConstraints.gridwidth = 1;
			optionsConstraints.gridheight = 1;
			optionsConstraints.gridx = 0;
			optionsConstraints.gridy = 0;
			optionsConstraints.insets = new Insets(5, 5, 6, 6);
			optionsJPanel.setLayout(optionsLayout);
                        
                        optionsJPanel.add( sigmaSlider,optionsConstraints);
                        optionsConstraints.gridy++;
                        optionsJPanel.add(sigmaText,optionsConstraints);
                        optionsConstraints.gridy++;
                        
                        
			// Buttons panel (including training and options)
			GridBagLayout buttonsLayout = new GridBagLayout();
			GridBagConstraints buttonsConstraints = new GridBagConstraints();
			buttonsPanel.setLayout(buttonsLayout);
			buttonsConstraints.anchor = GridBagConstraints.NORTHWEST;
			buttonsConstraints.fill = GridBagConstraints.VERTICAL;
			buttonsConstraints.gridwidth = 1;
			buttonsConstraints.gridheight = 1;
			buttonsConstraints.gridx = 0;
			buttonsConstraints.gridy = 0;
			buttonsPanel.add(controlJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsPanel.add(optionsJPanel, buttonsConstraints);
			buttonsConstraints.gridy++;
			buttonsConstraints.insets = new Insets(5, 5, 6, 6);
                     
                         
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints allConstraints = new GridBagConstraints();
			all.setLayout(layout);

			allConstraints.anchor = GridBagConstraints.NORTHWEST;
			allConstraints.fill = GridBagConstraints.BOTH;
			allConstraints.gridwidth = 1;
			allConstraints.gridheight = 1;
			allConstraints.gridx = 0;
			allConstraints.gridy = 0;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			all.add(buttonsPanel, allConstraints);
                       
                        
			allConstraints.gridx++;
			allConstraints.weightx = 1;
			allConstraints.weighty = 1;
			all.add(canvas, allConstraints);

			allConstraints.gridx++;
			allConstraints.anchor = GridBagConstraints.NORTHEAST;
			allConstraints.weightx = 0;
			allConstraints.weighty = 0;
			all.add(annotationsPanel, allConstraints);

			GridBagLayout wingb = new GridBagLayout();
			GridBagConstraints winc = new GridBagConstraints();
			winc.anchor = GridBagConstraints.NORTHWEST;
			winc.fill = GridBagConstraints.BOTH;
			winc.weightx = 1;
			winc.weighty = 1;
			setLayout(wingb);
			add(all, winc);

			
			// Propagate all listeners
			for (Component p : new Component[]{all, buttonsPanel}) {
				for (KeyListener kl : getKeyListeners()) {
					p.addKeyListener(kl);
				}
			}

			addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent e) {
					//IJ.log("closing window");
					// cleanup
					exec.shutdownNow();
					
				}
			});

			canvas.addComponentListener(new ComponentAdapter() {
				public void componentResized(ComponentEvent ce) {
//                                    System.out.println("("+rBkg.width +" "+ rBkg.height+")");
					r = canvas.getBounds();
					canvas.setDstDimensions(r.width, r.height);
                                         
//                                        
				}
			});
                         r = canvas.getBounds();
		}
                
            @Override
            public void setImage(ImagePlus imp2) {
                super.setImage(imp2); //To change body of generated methods, choose Tools | Templates.
                this.loadeImp = imp2;
                repaintAll();
            }
            
            /**
             * Repaint all panels
             */
            public void repaintAll()
            {
                    this.annotationsPanel.repaint();
                    getCanvas().repaint();
                    this.buttonsPanel.repaint();
                    this.all.repaint(); 
            }
		
	}
    

    
    private class DoubleJSlider extends JSlider {

        final int scale;

        private DoubleJSlider(int min, int max, int value, int scale) {
            super(min, max, value);
            this.scale = scale;
        }

        public double getScaledValue() {
            return ((double)super.getValue()) / this.scale;
        }
    }
    
}
