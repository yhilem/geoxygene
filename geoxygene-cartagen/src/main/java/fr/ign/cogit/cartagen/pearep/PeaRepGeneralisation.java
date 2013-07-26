/*******************************************************************************
 * This software is released under the licence CeCILL
 * 
 * see Licence_CeCILL-C_fr.html see Licence_CeCILL-C_en.html
 * 
 * see <a href="http://www.cecill.info/">http://www.cecill.info/a>
 * 
 * @copyright IGN
 ******************************************************************************/
package fr.ign.cogit.cartagen.pearep;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import org.geotools.data.shapefile.shp.ShapefileException;
import org.w3c.dom.DOMException;
import org.xml.sax.SAXException;

import fr.ign.cogit.cartagen.core.defaultschema.DefaultCreationFactory;
import fr.ign.cogit.cartagen.genealgorithms.landuse.LanduseSimplification;
import fr.ign.cogit.cartagen.pearep.derivation.ScaleMasterScheduler;
import fr.ign.cogit.cartagen.pearep.importexport.MGCPLoader;
import fr.ign.cogit.cartagen.pearep.importexport.SHOMLoader;
import fr.ign.cogit.cartagen.pearep.importexport.ShapeFileExport;
import fr.ign.cogit.cartagen.pearep.importexport.VMAP0Loader;
import fr.ign.cogit.cartagen.pearep.importexport.VMAP1Loader;
import fr.ign.cogit.cartagen.pearep.importexport.VMAP1PlusPlusLoader;
import fr.ign.cogit.cartagen.pearep.importexport.VMAP2Loader;
import fr.ign.cogit.cartagen.pearep.mgcp.MGCPSchemaFactory;
import fr.ign.cogit.cartagen.pearep.shom.SHOMSchemaFactory;
import fr.ign.cogit.cartagen.pearep.vmap.VMAPSchemaFactory;
import fr.ign.cogit.cartagen.pearep.vmap1PlusPlus.VMAP1PPSchemaFactory;
import fr.ign.cogit.cartagen.software.CartagenApplication;
import fr.ign.cogit.cartagen.software.dataset.CartAGenDoc;
import fr.ign.cogit.cartagen.software.dataset.PostgisDB;
import fr.ign.cogit.cartagen.software.dataset.SourceDLM;
import fr.ign.cogit.cartagen.software.interfacecartagen.interfacecore.Legend;
import fr.ign.cogit.cartagen.software.interfacecartagen.symbols.SymbolGroup;
import fr.ign.cogit.cartagen.software.interfacecartagen.symbols.SymbolsUtil;
import fr.ign.cogit.geoxygene.api.feature.IFeature;
import fr.ign.cogit.geoxygene.api.feature.IFeatureCollection;

/**
 * The class that contains the main application for generalisation in PEA REP
 * project.
 * @author GTouya
 * 
 */
public class PeaRepGeneralisation implements PropertyChangeListener,
    ActionListener {

  private JProgressBar progressBar;
  JFrame frame;
  private boolean stop;
  public static Logger errorLogger = Logger.getLogger("PeaRep.error.scheduler");

  public PeaRepGeneralisation(JFrame frame, JProgressBar progressBar) {
    this.progressBar = progressBar;
    this.frame = frame;
    this.setStop(false);
  }

  /**
   * @param args
   */
  public static void main(String[] args) {

    // load dlls
    System.loadLibrary("triangulation");

    // create a progress bar
    JFrame frame = new JFrame("Progression de la généralisation");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Create and set up the content pane.
    JPanel newContentPane = new JPanel();
    JProgressBar progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setStringPainted(true);
    JButton stopBtn = new JButton("Stop");
    stopBtn.setActionCommand("stop");
    newContentPane.add(Box.createVerticalGlue());
    newContentPane.add(progressBar);
    newContentPane.add(Box.createVerticalGlue());
    newContentPane.add(stopBtn);
    newContentPane.add(Box.createVerticalGlue());
    newContentPane.setLayout(new BoxLayout(newContentPane, BoxLayout.Y_AXIS));
    newContentPane.setOpaque(true); // content panes must be opaque
    frame.setContentPane(newContentPane);
    frame.setSize(400, 150);

    // Display the window.
    frame.pack();
    frame.setVisible(true);

    PeaRepGeneralisation main = new PeaRepGeneralisation(frame, progressBar);
    GeneralisationTask task = new GeneralisationTask(main);
    task.addPropertyChangeListener(main);
    stopBtn.addActionListener(main);
    try {
      task.execute();
    } catch (Exception e) {
      frame.setVisible(false);
      PeaRepGeneralisation.errorLogger.severe(e.getMessage());
      for (int i = 0; i < e.getStackTrace().length; i++) {
        PeaRepGeneralisation.errorLogger
            .severe(e.getStackTrace()[i].toString());
      }
    }
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    if ("progress" == evt.getPropertyName()) {
      int progress = (Integer) evt.getNewValue();
      this.progressBar.setValue(progress);
      if (progress == 100) {
        this.frame.setVisible(false);
        System.exit(0);
      }
    }
  }

  public void setStop(boolean stop) {
    this.stop = stop;
  }

  public boolean isStop() {
    return this.stop;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    this.setStop(true);
  }
}

class GeneralisationTask extends SwingWorker<Void, Void> {

  private PeaRepGeneralisation main;
  private static String SCALE_MASTER_FILE = "ScaleMaster.xml";
  private static String PARAMETER_FILE = "PeaRepParameters.xml";
  private static String THEMES_FILE = "ScaleMasterThemes.xml";
  private static String VMAP0_DATASET = "VMAP0";
  private static String VMAP1_DATASET = "VMAP1";
  private static String VMAP2i_DATASET = "VMAP2i";
  private static String MGCPPlusPlus_DATASET = "MGCPPlusPlus";
  private static String VMAP1PlusPlus_DATASET = "VMAP1PlusPlus";
  private static String SHOM_DATASET = "SHOM";
  private static Logger logger = Logger.getLogger(PeaRepGeneralisation.class
      .getName());

  public GeneralisationTask(PeaRepGeneralisation main) {
    super();
    this.main = main;
  }

  /*
   * Main task. Executed in background thread.
   */
  @Override
  public Void doInBackground() {
    try {
      // Initialize progress property.
      this.setProgress(0);
      int progress = 0;

      // ******************************************************
      // launch CartAGen as batch application
      // Objects creation factory
      CartagenApplication.getInstance().setCreationFactory(
          new DefaultCreationFactory());

      // Application initialisation
      CartagenApplication.getInstance().initApplication();
      CartAGenDoc doc = CartAGenDoc.getInstance();
      doc.setName("PEA_REP");
      doc.setPostGisDb(PostgisDB.get("PEA_REP", true));
      progress += 5;
      this.setProgress(progress);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // *******************************************************
      // first, create the scheduler by parsing the configuration files
      String jarPath = new File(PeaRepGeneralisation.class
          .getProtectionDomain().getCodeSource().getLocation().toURI()
          .getPath().substring(1)).getParent();
      String pathScale = jarPath + "\\" + GeneralisationTask.SCALE_MASTER_FILE;
      String pathParams = jarPath + "\\" + GeneralisationTask.PARAMETER_FILE;
      String pathThemes = jarPath + "\\" + GeneralisationTask.THEMES_FILE;

      // JOptionPane.showMessageDialog(null, jarPath);

      File scaleMasterXml = new File(pathScale);
      File parameterXml = new File(pathParams);
      File themesFile = new File(pathThemes);
      ScaleMasterScheduler scheduler = null;
      try {
        scheduler = new ScaleMasterScheduler(scaleMasterXml, parameterXml,
            themesFile);
      } catch (DOMException e) {
        GeneralisationTask.logger.severe("Problem in creating the scheduler");
        e.printStackTrace();
      } catch (ParserConfigurationException e) {
        GeneralisationTask.logger.severe("Problem in creating the scheduler");
        e.printStackTrace();
      } catch (SAXException e) {
        GeneralisationTask.logger.severe("Problem in creating the scheduler");
        e.printStackTrace();
      } catch (IOException e) {
        GeneralisationTask.logger.severe("Problem in creating the scheduler");
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        GeneralisationTask.logger.severe("Problem in creating the scheduler");
        e.printStackTrace();
      }
      if (scheduler == null) {
        return null;
      }
      this.setProgress(10);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // *******************************************************

      boolean vmap1ppDb = false;
      boolean vmapDb = false;
      boolean mgcpDb = false;
      boolean shomDb = false;

      // then, import all available databases
      SymbolGroup symbGroup = SymbolsUtil.getSymbolGroup(
          SourceDLM.SPECIAL_CARTAGEN, Legend.getSYMBOLISATI0N_SCALE());
      // first import the VMAP2i data in a new database
      if (scheduler.getVmap2iFolder() != null) {
        vmapDb = true;
        VMAP2Loader vmap2Loader = new VMAP2Loader(symbGroup,
            GeneralisationTask.VMAP2i_DATASET);
        try {
          vmap2Loader.loadData(new File(scheduler.getVmap2iFolder()),
              scheduler.getListLayersVmap2i());
        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP2i loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP2i loading");
          e1.printStackTrace();
        }
      }

      if (scheduler.getMgcpPlusPlusFolder() != null) {
        mgcpDb = true;
        MGCPLoader mgcpLoader = new MGCPLoader(symbGroup,
            GeneralisationTask.MGCPPlusPlus_DATASET);
        try {
          mgcpLoader.loadData(new File(scheduler.getMgcpPlusPlusFolder()),
              scheduler.getListLayersMgcpPlusPlus());
        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during MGCP loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during MGCP loading");
          e1.printStackTrace();
        }
      }

      if (scheduler.getShomFolder() != null) {
        shomDb = true;
        SHOMLoader shomLoader = new SHOMLoader(symbGroup,
            GeneralisationTask.SHOM_DATASET);
        try {
          shomLoader.loadData(new File(scheduler.getShomFolder()),
              scheduler.getListLayersMgcpPlusPlus());
          System.out.println(scheduler.getShomFolder());
          System.out.println(scheduler.getListLayersMgcpPlusPlus());

        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during SHOM loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during SHOM loading");
          e1.printStackTrace();
        }
      }

      this.setProgress(20);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // then, import the VMAP1 data
      if (scheduler.getVmap1Folder() != null) {
        vmapDb = true;
        VMAP1Loader vmap1Loader = new VMAP1Loader(symbGroup,
            GeneralisationTask.VMAP1_DATASET);
        try {
          vmap1Loader.loadData(new File(scheduler.getVmap1Folder()),
              scheduler.getListLayersVmap1());
        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP1 loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP1 loading");
          e1.printStackTrace();
        }
      }

      if (scheduler.getVmap1PlusPlusFolder() != null) {
        vmapDb = true;
        VMAP1PlusPlusLoader vmap1PlusPlusLoader = new VMAP1PlusPlusLoader(
            symbGroup, GeneralisationTask.VMAP1PlusPlus_DATASET);
        try {
          vmap1PlusPlusLoader.loadData(
              new File(scheduler.getVmap1PlusPlusFolder()),
              scheduler.getListLayersVmap1PlusPlus());
        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP1++ loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP1++ loading");
          e1.printStackTrace();
        }
      }
      this.setProgress(30);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // finally, import the VMAP0 data
      if (scheduler.getVmap0Folder() != null) {
        vmapDb = true;
        VMAP0Loader vmap0Loader = new VMAP0Loader(symbGroup,
            GeneralisationTask.VMAP0_DATASET);
        try {
          vmap0Loader.loadData(new File(scheduler.getVmap0Folder()),
              scheduler.getListLayersVmap0());
        } catch (ShapefileException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP0 loading");
          e1.printStackTrace();
        } catch (IOException e1) {
          GeneralisationTask.logger.severe("Problem during VMAP0 loading");
          e1.printStackTrace();
        }
      }
      this.setProgress(40);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // *******************************************************
      // set the SchemaFactory to the VMAP one

      if (vmap1ppDb == true) {
        CartagenApplication.getInstance().setCreationFactory(
            new VMAP1PPSchemaFactory());
      }
      if (vmapDb == true) {
        CartagenApplication.getInstance().setCreationFactory(
            new VMAPSchemaFactory());
      }
      if (mgcpDb == true) {
        CartagenApplication.getInstance().setCreationFactory(
            new MGCPSchemaFactory());
      }
      if (shomDb == true) {
        CartagenApplication.getInstance().setCreationFactory(
            new SHOMSchemaFactory());
      }

      // *******************************************************
      // trigger the generalisation
      try {
        scheduler.generalise();
      } catch (Exception e) {
        JOptionPane.showMessageDialog(null, e.getStackTrace());
      }
      this.setProgress(60);
      // Sleep for up to one second.
      try {
        if (this.main.isStop()) {
          this.setProgress(100);
        }
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }

      // *******************************************************
      // trigger landuse generalisation
      // get themes to be only exported by the landuse simplification process
      List<String> listThemeLanduse = new ArrayList<String>();
      Map<IFeatureCollection<IFeature>, String> mapFtColOut = new HashMap<IFeatureCollection<IFeature>, String>();
      if (!(scheduler.getMapLanduseParamIn().isEmpty())) {
        Map<IFeatureCollection<IFeature>, Map<String, Double>> mapFtColIn = scheduler
            .getMapLanduseParamIn();
        double dpFiltering = scheduler.getLanduseDpFilter();
        GeneralisationTask.logger
            .info("Début de la généralisation de l'occupation du sol");
        try {
          mapFtColOut = LanduseSimplification.landuseSimplify(mapFtColIn,
              dpFiltering);
        } catch (Exception e) {
          e.printStackTrace();
        }
        Iterator<IFeatureCollection<IFeature>> itFtCol = mapFtColIn.keySet()
            .iterator();
        while (itFtCol.hasNext()) {
          String nomTheme = mapFtColIn.get(itFtCol.next()).keySet().iterator()
              .next();
          listThemeLanduse.add(nomTheme);
        }
        this.setProgress(80);
      }

      // *******************************************************
      // finally, export data
      String exportPath = scheduler.getExportFolder();
      if (exportPath == null) {
        exportPath = jarPath;
      }
      ShapeFileExport exportTool = new ShapeFileExport(new File(exportPath),
          doc.getCurrentDataset(), scheduler.getScaleMaster(),
          scheduler.getScale());
      exportTool.setListThemesNotExport(listThemeLanduse);
      exportTool.exportToShapefiles();

      // export generalised landuse
      if (!mapFtColOut.isEmpty()) {
        exportTool.exportLanduseToShapefiles(mapFtColOut);
      }

      this.setProgress(100);
      // Sleep for up to one second.
      try {
        Thread.sleep(500);
      } catch (InterruptedException ignore) {
      }
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, e.getStackTrace());
      this.main.frame.setVisible(false);
    }
    return null;
  }

  /*
   * Executed in event dispatching thread
   */
  @Override
  public void done() {
  }
}
