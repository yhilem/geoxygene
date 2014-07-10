/*******************************************************************************
 * This file is part of the GeOxygene project source files.
 * 
 * GeOxygene aims at providing an open framework which implements OGC/ISO
 * specifications for the development and deployment of geographic (GIS)
 * applications. It is a open source contribution of the COGIT laboratory at the
 * Institut Géographique National (the French National Mapping Agency).
 * 
 * See: http://oxygene-project.sourceforge.net
 * 
 * Copyright (C) 2005 Institut Géographique National
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library (see file LICENSE if present); if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 * 02111-1307 USA
 *******************************************************************************/

package fr.ign.cogit.geoxygene.appli.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.text.DecimalFormat;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import fr.ign.cogit.geoxygene.appli.api.ProjectFrame;
import fr.ign.cogit.geoxygene.style.expressive.StrokeTextureExpressiveRendering;
import fr.ign.util.ui.SliderWithSpinner;
import fr.ign.util.ui.SliderWithSpinner.SliderWithSpinnerModel;

/**
 * @author JeT
 * 
 */
public class StrokeTextureExpressiveRenderingUI implements
        ExpressiveRenderingUI {

    private JPanel main = null;
    private StrokeTextureExpressiveRendering strtex = null;

    private final Preferences prefs = Preferences.userRoot();
    private ProjectFrame parentProjectFrame = null;
    double sampleSize = 2.;
    double minAngle = 1.5;
    double brushSize = 8;
    double paperScaleFactor = .5;
    double paperDensity = 0.7;
    double brushDensity = 1.9;
    double strokePressure = 2.64;
    double sharpness = 0.1;
    double strokePressureVariationAmplitude = .32;
    double strokePressureVariationWavelength = 100;
    double strokeShiftVariationAmplitude = .92;
    double strokeShiftVariationWavelength = 100;
    double strokeThicknessVariationAmplitude = .07;
    double strokeThicknessVariationWavelength = 100;
    public String paperTextureFilename = null;
    public String brushTextureFilename = null;
    public int brushStartLength = 100;
    public int brushEndLength = 200;
    private JLabel paperFilenameLabel = null;
    private JLabel brushFilenameLabel = null;
    private static final String LAST_DIRECTORY = StrokeTextureExpressiveRenderingUI.class
            .getSimpleName() + ".lastDirectory";
    private static final String PAPER_LAST_DIRECTORY = StrokeTextureExpressiveRenderingUI.class
            .getSimpleName() + ".paperLastDirectory";
    private static final String BRUSH_LAST_DIRECTORY = StrokeTextureExpressiveRenderingUI.class
            .getSimpleName() + ".brushLastDirectory";

    /**
     * Constructor
     */
    public StrokeTextureExpressiveRenderingUI(
            StrokeTextureExpressiveRendering strtex, ProjectFrame projectFrame) {
        this.parentProjectFrame = projectFrame;
        this.setStrokeTextureExpressiveRendering(strtex);
    }

    /**
     * set the managed stroke texture expressive rendering object
     * 
     * @param strtex
     */
    private void setStrokeTextureExpressiveRendering(
            StrokeTextureExpressiveRendering strtex) {
        this.strtex = strtex;
        this.setValuesFromObject();
    }

    /**
     * set variable values from stroke texture expressive rendering object
     */
    private void setValuesFromObject() {
        this.sampleSize = this.strtex.getSampleSize();
        this.minAngle = this.strtex.getMinAngle();
        this.brushSize = this.strtex.getBrushSize();
        this.paperScaleFactor = this.strtex.getPaperScaleFactor();
        this.paperDensity = this.strtex.getPaperDensity();
        this.brushDensity = this.strtex.getBrushDensity();
        this.strokePressure = this.strtex.getStrokePressure();
        this.sharpness = this.strtex.getSharpness();
        this.strokePressureVariationAmplitude = this.strtex
                .getStrokePressureVariationAmplitude();
        this.strokePressureVariationWavelength = this.strtex
                .getStrokePressureVariationWavelength();
        this.strokeShiftVariationAmplitude = this.strtex
                .getStrokeShiftVariationAmplitude();
        this.strokeShiftVariationWavelength = this.strtex
                .getStrokeShiftVariationWavelength();
        this.strokeThicknessVariationAmplitude = this.strtex
                .getStrokeThicknessVariationAmplitude();
        this.strokeThicknessVariationWavelength = this.strtex
                .getStrokeThicknessVariationWavelength();
        this.paperTextureFilename = this.strtex.getPaperTextureFilename();
        this.brushTextureFilename = this.strtex.getBrushTextureFilename();
        this.brushStartLength = this.strtex.getBrushStartLength();
        this.brushEndLength = this.strtex.getBrushEndLength();
    }

    /**
     * set variable values from stroke texture expressive rendering object
     */
    private void setValuesToObject() {
        this.strtex.setSampleSize(this.sampleSize);
        this.strtex.setMinAngle(this.minAngle);
        this.strtex.setBrushSize(this.brushSize);
        this.strtex.setPaperScaleFactor(this.paperScaleFactor);
        this.strtex.setPaperDensity(this.paperDensity);
        this.strtex.setBrushDensity(this.brushDensity);
        this.strtex.setStrokePressure(this.strokePressure);
        this.strtex.setSharpness(this.sharpness);
        this.strtex
                .setStrokePressureVariationAmplitude(this.strokePressureVariationAmplitude);
        this.strtex
                .setStrokePressureVariationWavelength(this.strokePressureVariationWavelength);
        this.strtex
                .setStrokeShiftVariationAmplitude(this.strokeShiftVariationAmplitude);
        this.strtex
                .setStrokeShiftVariationWavelength(this.strokeShiftVariationWavelength);
        this.strtex
                .setStrokeThicknessVariationAmplitude(this.strokeThicknessVariationAmplitude);
        this.strtex
                .setStrokeThicknessVariationWavelength(this.strokeThicknessVariationWavelength);
        this.strtex.setPaperTextureFilename(this.paperTextureFilename);
        this.strtex.setBrushTextureFilename(this.brushTextureFilename);
        this.strtex.setBrushStartLength(this.brushStartLength);
        this.strtex.setBrushEndLength(this.brushEndLength);
    }

    /*
     * (non-Javadoc)
     * 
     * @see fr.ign.cogit.geoxygene.appli.ui.ExpressiveRenderingUI#getGui()
     */
    @Override
    public JComponent getGui() {
        return new JScrollPane(this.getMainPanel());
    }

    protected void refresh() {
        this.setValuesToObject();
        this.parentProjectFrame.repaint();
    }

    private JPanel getMainPanel() {
        if (this.main == null) {
            this.main = new JPanel();
            this.main.setLayout(new BoxLayout(this.main, BoxLayout.Y_AXIS));
            this.main.setBorder(BorderFactory
                    .createEtchedBorder(EtchedBorder.LOWERED));
            // Dimension d = new Dimension(150, 40);
            SliderWithSpinnerModel model = new SliderWithSpinnerModel(
                    this.sampleSize, 0.1, 1000., 1.);
            final SliderWithSpinner spinner = new SliderWithSpinner(model);
            JSpinner.NumberEditor editor = (JSpinner.NumberEditor) spinner
                    .getEditor();
            DecimalFormat format = editor.getFormat();
            format.setMinimumFractionDigits(3);
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            spinner.setBorder(BorderFactory.createTitledBorder("sample size"));
            spinner.setToolTipText("distance between samples during line tesselation");
            spinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.sampleSize = (spinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(spinner);

            SliderWithSpinnerModel minAngleModel = new SliderWithSpinnerModel(
                    this.minAngle, 0, 180, .1);
            final SliderWithSpinner minAngleSpinner = new SliderWithSpinner(
                    minAngleModel);
            JSpinner.NumberEditor minAngleEditor = (JSpinner.NumberEditor) minAngleSpinner
                    .getEditor();
            minAngleEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            minAngleSpinner.setBorder(BorderFactory
                    .createTitledBorder("min angle"));
            minAngleSpinner
                    .setToolTipText("minimum angle in tesselation under which edges are considered colinear");
            minAngleSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.minAngle = (minAngleSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(minAngleSpinner);

            SliderWithSpinnerModel brushSizeModel = new SliderWithSpinnerModel(
                    this.brushSize, 0, 180, .1);
            final SliderWithSpinner brushSizeSpinner = new SliderWithSpinner(
                    brushSizeModel);
            JSpinner.NumberEditor brushSizeEditor = (JSpinner.NumberEditor) brushSizeSpinner
                    .getEditor();
            brushSizeEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            brushSizeSpinner.setBorder(BorderFactory
                    .createTitledBorder("brush size"));
            brushSizeSpinner
                    .setToolTipText("size of one pixel of the brush (in mm)");

            brushSizeSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.brushSize = (brushSizeSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(brushSizeSpinner);

            JButton paperBrowseButton = new JButton("paper browse...");
            paperBrowseButton.setBorder(BorderFactory.createEmptyBorder(2, 2,
                    2, 2));
            paperBrowseButton.setToolTipText("Load background paper file");
            paperBrowseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(
                            StrokeTextureExpressiveRenderingUI.this.prefs.get(
                                    PAPER_LAST_DIRECTORY, "."));
                    if (fc.showOpenDialog(StrokeTextureExpressiveRenderingUI.this.parentProjectFrame
                            .getGui()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            File selectedFile = fc.getSelectedFile();
                            StrokeTextureExpressiveRenderingUI.this.paperTextureFilename = selectedFile
                                    .getAbsolutePath();
                            StrokeTextureExpressiveRenderingUI.this.paperFilenameLabel
                                    .setText(StrokeTextureExpressiveRenderingUI.this.paperTextureFilename
                                            .substring(StrokeTextureExpressiveRenderingUI.this.paperTextureFilename
                                                    .length() - 30));

                            StrokeTextureExpressiveRenderingUI.this.prefs.put(
                                    PAPER_LAST_DIRECTORY,
                                    selectedFile.getAbsolutePath());

                            StrokeTextureExpressiveRenderingUI.this.refresh();
                        } catch (Exception e1) {
                            JOptionPane
                                    .showMessageDialog(
                                            StrokeTextureExpressiveRenderingUI.this.parentProjectFrame
                                                    .getGui(), e1.getMessage());
                            e1.printStackTrace();
                        }
                    }
                }

            });
            this.main.add(paperBrowseButton);
            this.paperFilenameLabel = new JLabel(
                    this.paperTextureFilename
                            .substring(this.paperTextureFilename.length() - 30));
            this.main.add(this.paperFilenameLabel);

            SliderWithSpinnerModel paperScaleFactorModel = new SliderWithSpinnerModel(
                    this.paperScaleFactor, 0, 180, .1);
            final SliderWithSpinner paperScaleFactorSpinner = new SliderWithSpinner(
                    paperScaleFactorModel);
            JSpinner.NumberEditor paperScaleFactorEditor = (JSpinner.NumberEditor) paperScaleFactorSpinner
                    .getEditor();
            paperScaleFactorEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            paperScaleFactorSpinner.setBorder(BorderFactory
                    .createTitledBorder("paper scale"));
            paperScaleFactorSpinner
                    .setToolTipText("paper texture scale factor");
            paperScaleFactorSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.paperScaleFactor = (paperScaleFactorSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(paperScaleFactorSpinner);

            SliderWithSpinnerModel brushStartModel = new SliderWithSpinnerModel(
                    this.brushStartLength, 1, 5000, 1);
            final SliderWithSpinner brushStartSpinner = new SliderWithSpinner(
                    brushStartModel);
            JSpinner.NumberEditor brushStartEditor = (JSpinner.NumberEditor) brushStartSpinner
                    .getEditor();
            DecimalFormat intFormat = brushStartEditor.getFormat();
            intFormat.setMinimumFractionDigits(0);
            brushStartEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            brushStartSpinner.setBorder(BorderFactory
                    .createTitledBorder("brush start"));
            brushStartSpinner.setToolTipText("length of the brush start");
            brushStartSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.brushStartLength = (int) (double) (brushStartSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });

            SliderWithSpinnerModel brushEndModel = new SliderWithSpinnerModel(
                    this.brushEndLength, 1, 5000, 1);
            final SliderWithSpinner brushEndSpinner = new SliderWithSpinner(
                    brushEndModel);
            JSpinner.NumberEditor brushEndEditor = (JSpinner.NumberEditor) brushEndSpinner
                    .getEditor();
            intFormat.setMinimumFractionDigits(0);
            brushEndEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            brushEndSpinner.setBorder(BorderFactory
                    .createTitledBorder("brush end"));
            brushEndSpinner.setToolTipText("length of the brush end");
            brushEndSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.brushEndLength = (int) (double) (brushEndSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });

            JButton brushBrowseButton = new JButton("brush browser...");
            brushBrowseButton.setBorder(BorderFactory.createEmptyBorder(2, 2,
                    2, 2));
            brushBrowseButton.setToolTipText("Load brush file");
            brushBrowseButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    JFileChooser fc = new JFileChooser(
                            StrokeTextureExpressiveRenderingUI.this.prefs.get(
                                    BRUSH_LAST_DIRECTORY, "."));
                    if (fc.showOpenDialog(StrokeTextureExpressiveRenderingUI.this.parentProjectFrame
                            .getGui()) == JFileChooser.APPROVE_OPTION) {
                        try {
                            File selectedFile = fc.getSelectedFile();
                            StrokeTextureExpressiveRenderingUI.this.brushTextureFilename = selectedFile
                                    .getAbsolutePath();
                            StrokeTextureExpressiveRenderingUI.this.brushFilenameLabel
                                    .setText(StrokeTextureExpressiveRenderingUI.this.brushTextureFilename
                                            .substring(StrokeTextureExpressiveRenderingUI.this.brushTextureFilename
                                                    .length() - 30));
                            Pattern pattern = Pattern
                                    .compile("([0-9]+)-([0-9]+)");
                            Matcher matcher = pattern
                                    .matcher(StrokeTextureExpressiveRenderingUI.this.brushTextureFilename);
                            if (matcher.matches()) {
                                int start = Integer.valueOf(matcher.group(1));
                                int end = Integer.valueOf(matcher.group(2));
                                brushStartSpinner.setValue(start);
                                brushEndSpinner.setValue(end);
                                StrokeTextureExpressiveRenderingUI.this.brushStartLength = (int) (double) (brushStartSpinner
                                        .getValue());
                                StrokeTextureExpressiveRenderingUI.this.brushEndLength = (int) (double) (brushEndSpinner
                                        .getValue());
                            }
                            StrokeTextureExpressiveRenderingUI.this.prefs.put(
                                    BRUSH_LAST_DIRECTORY,
                                    selectedFile.getAbsolutePath());
                            StrokeTextureExpressiveRenderingUI.this.refresh();
                        } catch (Exception e1) {
                            JOptionPane
                                    .showMessageDialog(
                                            StrokeTextureExpressiveRenderingUI.this.parentProjectFrame
                                                    .getGui(), e1.getMessage());
                            e1.printStackTrace();
                        }
                    }
                }

            });
            this.main.add(brushBrowseButton);

            this.brushFilenameLabel = new JLabel(
                    this.brushTextureFilename
                            .substring(this.brushTextureFilename.length() - 30));
            this.main.add(this.brushFilenameLabel);

            this.main.add(brushStartSpinner);
            this.main.add(brushEndSpinner);

            SliderWithSpinnerModel brushDensityModel = new SliderWithSpinnerModel(
                    this.brushDensity, 0, 10, .1);
            final SliderWithSpinner brushDensitySpinner = new SliderWithSpinner(
                    brushDensityModel);
            JSpinner.NumberEditor brushDensityEditor = (JSpinner.NumberEditor) brushDensitySpinner
                    .getEditor();
            brushDensityEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            brushDensitySpinner.setBorder(BorderFactory
                    .createTitledBorder("brush density"));
            brushDensitySpinner.setToolTipText("brush height scale factor");
            brushDensitySpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.brushDensity = (brushDensitySpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(brushDensitySpinner);

            SliderWithSpinnerModel paperDensityModel = new SliderWithSpinner.SliderWithSpinnerModel(
                    this.paperDensity, 0, 10, 0.1, .001);
            final SliderWithSpinner paperDensitySpinner = new SliderWithSpinner(
                    paperDensityModel);
            JSpinner.NumberEditor paperDensityEditor = (JSpinner.NumberEditor) paperDensitySpinner
                    .getEditor();
            paperDensityEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            paperDensitySpinner.setBorder(BorderFactory
                    .createTitledBorder("paper density"));
            paperDensitySpinner.setToolTipText("scale factor for paper height");
            paperDensitySpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.paperDensity = (paperDensitySpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(paperDensitySpinner);

            SliderWithSpinnerModel pressureModel = new SliderWithSpinnerModel(
                    this.strokePressure, 0.01, 100, .01);
            final SliderWithSpinner pressureSpinner = new SliderWithSpinner(
                    pressureModel);
            JSpinner.NumberEditor pressureEditor = (JSpinner.NumberEditor) pressureSpinner
                    .getEditor();
            pressureEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            pressureSpinner.setBorder(BorderFactory
                    .createTitledBorder("stroke pressure"));
            pressureSpinner.setToolTipText("distance between brush and paper");
            pressureSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.strokePressure = (pressureSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(pressureSpinner);
            SliderWithSpinnerModel sharpnessModel = new SliderWithSpinnerModel(
                    this.sharpness, 0.0001, 10, .001);
            final SliderWithSpinner sharpnessSpinner = new SliderWithSpinner(
                    sharpnessModel);
            JSpinner.NumberEditor sharpnessEditor = (JSpinner.NumberEditor) sharpnessSpinner
                    .getEditor();
            sharpnessEditor.getTextField().setHorizontalAlignment(
                    SwingConstants.CENTER);
            sharpnessSpinner.setBorder(BorderFactory
                    .createTitledBorder("blending sharpness"));
            sharpnessSpinner
                    .setToolTipText("blending contrast between brush and paper");
            sharpnessSpinner.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    StrokeTextureExpressiveRenderingUI.this.sharpness = (sharpnessSpinner
                            .getValue());
                    StrokeTextureExpressiveRenderingUI.this.refresh();

                }
            });
            this.main.add(sharpnessSpinner);

            SliderWithSpinnerModel pressureVariationAmplitudeModel = new SliderWithSpinnerModel(
                    this.strokePressureVariationAmplitude, 0, 100, .1);
            final SliderWithSpinner pressureVariationAmplitudeSpinner = new SliderWithSpinner(
                    pressureVariationAmplitudeModel);
            JSpinner.NumberEditor pressureVariationAmplitudeEditor = (JSpinner.NumberEditor) pressureVariationAmplitudeSpinner
                    .getEditor();
            pressureVariationAmplitudeEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            pressureVariationAmplitudeSpinner.setBorder(BorderFactory
                    .createTitledBorder("pressure amplitude"));

            pressureVariationAmplitudeSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokePressureVariationAmplitude = (pressureVariationAmplitudeSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(pressureVariationAmplitudeSpinner);

            SliderWithSpinnerModel pressureVariationWavelengthModel = new SliderWithSpinnerModel(
                    this.strokePressureVariationWavelength, 0.001, 100000, 10);
            final SliderWithSpinner pressureVariationWavelengthSpinner = new SliderWithSpinner(
                    pressureVariationWavelengthModel);
            JSpinner.NumberEditor pressureVariationWavelengthEditor = (JSpinner.NumberEditor) pressureVariationWavelengthSpinner
                    .getEditor();
            pressureVariationWavelengthEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            pressureVariationWavelengthSpinner.setBorder(BorderFactory
                    .createTitledBorder("pressure Wavelength"));

            pressureVariationWavelengthSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokePressureVariationWavelength = (pressureVariationWavelengthSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(pressureVariationWavelengthSpinner);

            SliderWithSpinnerModel shiftVariationAmplitudeModel = new SliderWithSpinnerModel(
                    this.strokeShiftVariationAmplitude, 0, 1, .01);
            final SliderWithSpinner shiftVariationAmplitudeSpinner = new SliderWithSpinner(
                    shiftVariationAmplitudeModel);
            JSpinner.NumberEditor shiftVariationAmplitudeEditor = (JSpinner.NumberEditor) shiftVariationAmplitudeSpinner
                    .getEditor();
            shiftVariationAmplitudeEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            shiftVariationAmplitudeSpinner.setBorder(BorderFactory
                    .createTitledBorder("shift amplitude"));

            shiftVariationAmplitudeSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokeShiftVariationAmplitude = (shiftVariationAmplitudeSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(shiftVariationAmplitudeSpinner);

            SliderWithSpinnerModel shiftVariationWavelengthModel = new SliderWithSpinnerModel(
                    this.strokeShiftVariationWavelength, 0.001, 100000, 10);
            final SliderWithSpinner shiftVariationWavelengthSpinner = new SliderWithSpinner(
                    shiftVariationWavelengthModel);
            JSpinner.NumberEditor shiftVariationWavelengthEditor = (JSpinner.NumberEditor) shiftVariationWavelengthSpinner
                    .getEditor();
            shiftVariationWavelengthEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            shiftVariationWavelengthSpinner.setBorder(BorderFactory
                    .createTitledBorder("shift Wavelength"));

            shiftVariationWavelengthSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokeShiftVariationWavelength = (shiftVariationWavelengthSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(shiftVariationWavelengthSpinner);

            SliderWithSpinnerModel thicknessVariationAmplitudeModel = new SliderWithSpinnerModel(
                    this.strokeThicknessVariationAmplitude, 0, 1, .01);
            final SliderWithSpinner thicknessVariationAmplitudeSpinner = new SliderWithSpinner(
                    thicknessVariationAmplitudeModel);
            JSpinner.NumberEditor thicknessVariationAmplitudeEditor = (JSpinner.NumberEditor) thicknessVariationAmplitudeSpinner
                    .getEditor();
            thicknessVariationAmplitudeEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            thicknessVariationAmplitudeSpinner.setBorder(BorderFactory
                    .createTitledBorder("thickness amplitude"));

            thicknessVariationAmplitudeSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokeThicknessVariationAmplitude = (thicknessVariationAmplitudeSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(thicknessVariationAmplitudeSpinner);

            SliderWithSpinnerModel thicknessVariationWavelengthModel = new SliderWithSpinnerModel(
                    this.strokeThicknessVariationWavelength, 0.001, 100000, 10);
            final SliderWithSpinner thicknessVariationWavelengthSpinner = new SliderWithSpinner(
                    thicknessVariationWavelengthModel);
            JSpinner.NumberEditor thicknessVariationWavelengthEditor = (JSpinner.NumberEditor) thicknessVariationWavelengthSpinner
                    .getEditor();
            thicknessVariationWavelengthEditor.getTextField()
                    .setHorizontalAlignment(SwingConstants.CENTER);
            thicknessVariationWavelengthSpinner.setBorder(BorderFactory
                    .createTitledBorder("thickness Wavelength"));

            thicknessVariationWavelengthSpinner
                    .addChangeListener(new ChangeListener() {

                        @Override
                        public void stateChanged(ChangeEvent e) {
                            StrokeTextureExpressiveRenderingUI.this.strokeThicknessVariationWavelength = (thicknessVariationWavelengthSpinner
                                    .getValue());
                            StrokeTextureExpressiveRenderingUI.this.refresh();

                        }
                    });
            this.main.add(thicknessVariationWavelengthSpinner);
        }
        return this.main;
    }
}