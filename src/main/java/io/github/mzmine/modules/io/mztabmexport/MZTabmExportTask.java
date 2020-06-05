/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.io.mztabmexport;

import de.isas.mztab2.io.MzTabNonValidatingWriter;
import de.isas.mztab2.model.*;
import io.github.mzmine.datamodel.*;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import uk.ac.ebi.pride.jmztab2.model.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Pattern;

public class MZTabmExportTask extends AbstractTask {

    private int processedRows = 0, totalRows = 0;

    // parameter values
    private final MZmineProject project;
    private final File fileName;
    private String plNamePattern = "{}";
    private PeakList[] peakLists;
    private final boolean exportAll;

    MZTabmExportTask(MZmineProject project, ParameterSet parameters){
        this.project = project;
        this.peakLists =
            parameters.getParameter(MZTabmExportParameters.peakLists).getValue().getMatchingPeakLists();
        this.fileName = parameters.getParameter(MZTabmExportParameters.filename).getValue();
        this.exportAll = parameters.getParameter(MZTabmExportParameters.exportAll).getValue();
    }

    @Override
    public double getFinishedPercentage() {
        if (totalRows == 0) {
            return 0;
        }
        return (double) processedRows / (double) totalRows;
    }

    @Override
    public String getTaskDescription() {
        return "Exporting feature list(s) " + Arrays.toString(peakLists) + " to MzTab-m file(s)";
    }

    @Override
    public void run(){
        setStatus(TaskStatus.PROCESSING);

        //TODO Shall export several files?
        boolean substitute = fileName.getPath().contains(plNamePattern);

        // Total Number of rows
        for (PeakList peakList : peakLists){
            totalRows += peakList.getNumberOfRows();
        }

        //Process feature Lists
        for(PeakList peakList: peakLists){
            File curFile = fileName;
            try{
                //Filename
                if(substitute){
                    // Cleanup from illegal filename characters
                    //not small alphabets, large alphabets, numbers, dots or dashes
                    String cleanPlName = peakList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
                    // Substitute
                    String newFilename =
                            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
                    curFile = new File(newFilename);
                }

                MzTab mzTabFile = new MzTab();


                //Metadata
                Metadata mtd= new Metadata();
                mtd.setMzTabVersion("2.0.0-M");
                //TODO mzTabID
                mtd.setMzTabID("1");
                mtd.setDescription(peakList.getName());
                mtd.addSoftwareItem(new Software().id(1).parameter(
                        new Parameter().cvLabel("MS").cvAccession("MS:1002342").name("MZmine").value(MZmineCore.getMZmineVersion())));
                mtd.setSmallMoleculeQuantificationUnit(new Parameter().cvLabel("PRIDE").cvAccession("PRIDE:0000330").name("Arbitrary quantification unit"));
                mtd.addIdConfidenceMeasureItem(new Parameter().cvLabel("MS").cvAccession("MS:1001153").name("search engine specific score"));
//                TODO addFixedModParam
//                mtd.(new Parameter().cvLabel("MS").cvAccession("MS:1002453").name("No fixed modifications searched"));
//                TODO addVariableModParam
//                mtd.(new Parameter().cvLabel("MS").cvAccession("MS:1002454").name("No variable modifications searched"));

                List<IOptColumnMappingBuilder> peak_mzList = new ArrayList<>();
                List<IOptColumnMappingBuilder> peak_rtList = new ArrayList<>();
                List<IOptColumnMappingBuilder> peak_heightList = new ArrayList<>();

                final RawDataFile rawDataFiles[] = peakList.getRawDataFiles().toArray(RawDataFile[]::new);
                int fileCounter = 0;
                // Study Variable name and descriptions
                Hashtable<String, List<RawDataFile>> svhash = new Hashtable<>();
                Hashtable<RawDataFile, Assay> rawDataFileToAssay = new Hashtable<>();

                for(RawDataFile file : rawDataFiles){
                    fileCounter++;
                    /**
                     * TO DO: Add path to original imported raw file to MZmine and write it out here instead
                     */
                    // MS run location
                    MsRun msRun = new MsRun();
                    msRun.id(fileCounter);
                    msRun.setLocation("file://"+file.getName());
                    mtd.addMsRunItem(msRun);
                    // Add Assay
                    Assay assay = new Assay();
                    rawDataFileToAssay.put(file,assay);
                    assay.id(fileCounter);
                    assay.addMsRunRefItem(msRun);
                    //TODO add sample ref for assay, quantification  reagent?
                    mtd.addAssayItem(assay);

                    for(UserParameter<?,?> p: project.getParameters()){
                        if(p.getName().contains("study variable")){
                            if(svhash.containsKey(String.valueOf(project.getParameterValue(p, file)))){
                                svhash.get(String.valueOf(project.getParameterValue(p,file))).add(file);
                            }
                            else{
                                List<RawDataFile> l = new ArrayList<>();
                                l.add(file);
                                svhash.put(String.valueOf(project.getParameterValue(p,file)), l);
                            }
                            break;
                        }
                    }


                    //Optional Columns
                    peak_mzList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_mz"));
                    peak_rtList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_rt"));
                    peak_heightList.add(OptColumnMappingBuilder.forIndexedElement(assay).withName("peak_height"));
                }
                int studyVarCount = 0;
                for(String key: svhash.keySet()){
                    studyVarCount++;
                    StudyVariable studyVariable = new StudyVariable().id(studyVarCount).name(key).description(key).
                            averageFunction(new Parameter().cvLabel("MS").cvAccession("MS:1002883").name("mean"));
                    //TODO check cvAccession
                    for(RawDataFile file : svhash.get(key)){
                        studyVariable =  studyVariable.addAssayRefsItem(rawDataFileToAssay.get(file));
                    }
                    mtd.addStudyVariableItem(studyVariable);
                }

                mzTabFile.metadata(mtd);

                //Write data rows
                for(int i=0;i<peakList.getRows().size();++i){
                    PeakListRow peakListRow = peakList.getRows().get(i);
                    SmallMoleculeSummary sm = new SmallMoleculeSummary();
                    sm.setSmlId(i+1);
                    //TODO multiple smfs for each sml
                    SmallMoleculeFeature smf = new SmallMoleculeFeature();
                    smf.setSmfId(i+1);
                    //TODO muliple smes for each smfs and vice versa
                    SmallMoleculeEvidence sme = new SmallMoleculeEvidence();
                    sme.setSmeId(i+1);
                    //Cancelled?
                    if(isCanceled()){
                        return;
                    }
                    PeakIdentity peakIdentity = peakListRow.getPreferredPeakIdentity();
                    if(exportAll || peakIdentity !=null){
                        if(peakIdentity != null){
                            //Identity Information
                            String identifier = escapeString(peakIdentity.getPropertyValue("ID"));
                            String method = peakIdentity.getPropertyValue("Identification method");
                            String formula = peakIdentity.getPropertyValue("Molecular formula");
                            String description = escapeString(peakIdentity.getPropertyValue("Name"));
                            String url = peakIdentity.getPropertyValue("URL");
                            if(identifier != null){
                                sm.addDatabaseIdentifierItem(identifier);
                                sme.setDatabaseIdentifier(identifier);
                            }
                            if(method != null){
                                sme.setIdentificationMethod(new Parameter().name("").value(method));
                            }
                            if(formula != null){
                                ArrayList<String> formulaList = new ArrayList<>();
                                formulaList.add(formula);
                                sm.setChemicalFormula(formulaList);
                                sme.setChemicalFormula(formula);
                            }
                            if(description != null){
                                ArrayList<String> chemicalName = new ArrayList<>();
                                chemicalName.add(description);
                                sm.setChemicalName(chemicalName);
                                sme.setChemicalName(description);
                            }
                            if(url!=null){
                                ArrayList<String> uris = new ArrayList<>();
                                uris.add(url);
                                sm.setUri(uris);
                                sme.setUri(url);
                            }
                        }

                        Double rowMZ = peakListRow .getAverageMZ();
                        int rowCharge = peakListRow.getRowCharge();
                        Double rowRT = peakListRow.getAverageRT();

                        if(rowMZ != null){
                            smf.setExpMassToCharge(rowMZ);
                        }
                        if(rowCharge > 0){
                            smf.setCharge(rowCharge);
                        }
                        if(rowRT != null){
                            smf.setRetentionTimeInSeconds(rowRT);
                        }
                        int dataFileCount = 0;
                        Hashtable<String, List<Double>> sampleVariableAbundancehash = new Hashtable<>();
                        for(RawDataFile dataFile : rawDataFiles){
                            dataFileCount++;
                            Feature peak = peakListRow.getPeak(dataFile);
                            if(peak != null){
                                String peakMZ = String.valueOf(peak.getMZ());
                                String peakRT = String.valueOf(peak.getRT());
                                String peakHeight = String.valueOf(peak.getHeight());
                                Double peakArea = peak.getArea();
                                sm.addOptItem(peak_mzList.get(dataFileCount-1).build(peakMZ));
                                sm.addOptItem(peak_rtList.get(dataFileCount-1).build(peakRT));
                                sm.addOptItem(peak_heightList.get(dataFileCount-1).build(peakHeight));
                                sm.addAbundanceAssayItem(peakArea);
                                smf.addAbundanceAssayItem(peakArea);
                                for(String sampleVariable : svhash.keySet()){
                                    if(svhash.get(sampleVariable).contains(dataFile)){
                                        if(sampleVariableAbundancehash.containsKey(sampleVariable)){
                                            sampleVariableAbundancehash.get(sampleVariable).add(peakArea);
                                        }
                                        else{
                                            List<Double> l = new ArrayList<>();
                                            l.add(peakArea);
                                            sampleVariableAbundancehash.put(sampleVariable,l);
                                        }
                                    }
                                }
                                //TODO sum of smf abundance assay to be used in sm
                            }
                        }
                        for(String studyVariable:sampleVariableAbundancehash.keySet()){
                            Double averageSV = 0.0;
                            //Using mean as average function for abundance of Study Variable
                            for(Double d : sampleVariableAbundancehash.get(studyVariable)){
                                averageSV+=d;
                            }
                            int totalSV =sampleVariableAbundancehash.get(studyVariable).size();
                            if(totalSV ==0){
                                averageSV = 0.0;
                            }
                            else {
                                averageSV /=totalSV;
                            }
                            //Coefficient of variation
                            Double covSV = 0.0;
                            for(Double d:sampleVariableAbundancehash.get(studyVariable)){
                                covSV += (d - averageSV)*(d - averageSV);
                            }
                            if(totalSV == 0 || totalSV == 1) {
                                covSV = 0.0;
                            }
                            else{
                                covSV/=(totalSV-1);
                                covSV = Math.sqrt(covSV);
                                if(averageSV != 0.0){
                                    covSV = covSV/averageSV *100.0;
                                }
                            }
                            sm.addAbundanceStudyVariableItem(averageSV);
                            sm.addAbundanceVariationStudyVariableItem(covSV);
                        }

                    }

                    sm.addSmfIdRefsItem(smf.getSmfId());
                    smf.addSmeIdRefsItem(sme.getSmeId());
                    mzTabFile.addSmallMoleculeSummaryItem(sm);
                    mzTabFile.addSmallMoleculeFeatureItem(smf);
                    mzTabFile.addSmallMoleculeEvidenceItem(sme);
                }
                //TODO non validating writer to validating writer
                MzTabNonValidatingWriter validatingWriter = new MzTabNonValidatingWriter();
//                MzTabValidatingWriter validatingWriter = new MzTabValidatingWriter();
                validatingWriter.write(curFile.toPath(),mzTabFile);
            } catch (Exception e){
                e.printStackTrace();
                setStatus(TaskStatus.ERROR);
                setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
                return;
            }
        }
        if(getStatus() == TaskStatus.PROCESSING){
            setStatus(TaskStatus.FINISHED);
        }
    }

    private String escapeString(final String inputString) {

        if (inputString == null)
            return "";

        // Remove all special characters e.g. \n \t
        return inputString.replaceAll("[\\p{Cntrl}]", " ");
    }


}
