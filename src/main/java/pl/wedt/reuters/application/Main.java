package pl.wedt.reuters.application;

import de.bwaldvogel.liblinear.SolverType;
import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations.DocumentDirectoryAnnotation;

import org.apache.log4j.Logger;
import pl.wedt.reuters.classifier.SVM;
import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.DocumentFiltered;
import pl.wedt.reuters.service.DocumentService;
import pl.wedt.reuters.service.EvaluationService;
import pl.wedt.reuters.utils.Category;
import pl.wedt.reuters.utils.PropertiesLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * @author Michał Żakowski
 *
 * Klasa startowa aplikacji
 */

public class Main {
    private final static Logger logger = Logger.getLogger(Main.class);
    private final static Integer DOCUMENT_NUMBER = 10;

    public static void main(String [] args) {
        String resourcesPath = Main.class.getClassLoader().getResource("reuters21578").getPath();

        try {
            Properties properties =
                    new PropertiesLoader(Main.class.getClassLoader().getResource("app.properties").getPath())
                        .getProperties();
            Category.loadData(resourcesPath);

            List<String> documentNames = new ArrayList<>();
            IntStream.range(0, DOCUMENT_NUMBER).forEach(i -> {
                documentNames.add("reut2-" + String.format("%03d", i) + ".sgm");
            });

            DocumentService documentService = new DocumentService(resourcesPath, documentNames, "stop_list.txt");
            logger.info("Ładowanie dokumentów");
            documentService.loadDocuments();
            logger.info("Przetwarzanie dokumentów");
            documentService.prepareDocuments();
            logger.info("Klasyfikacja SVM");
            
            //doCategorize(documentService, properties);
            
//            SVM svm = new SVM(Double.valueOf(properties.getProperty("svm.C")), Double.valueOf(properties.getProperty("svm.eps")), 
//            									documentService.getDim(), SolverType.L1R_L2LOSS_SVC);
//
//            EvaluationService evaluationService = new EvaluationService();
//            
//            List<DocumentFiltered> trainSet = getByCategorySize(documentService, CategoryType.PLACES, 40);
//            List<DocumentFiltered> testSet = trainSet;
//
//            List<Integer> clsLabels = svm.clsOVA(trainSet, testSet);
//            evaluationService.evaluate(testSet.stream().map(d -> d.getCategory()).collect(Collectors.toList()),
//                    clsLabels);
//
//            List<Integer> clsLabels2 = svm.clsOVO(trainSet, testSet);
//            evaluationService.evaluate(testSet.stream().map(d -> d.getCategory()).collect(Collectors.toList()),
//                    clsLabels2);

        } catch(Exception e) {
            e.printStackTrace();
        }
    }
    
    private static List<DocumentFiltered> getByCategorySize(DocumentService documentService, CategoryType categoryType, int minSize) {
        List<DocumentFiltered> docs = documentService.getTrainingDocuments();
        List<Integer> categories = docs.stream().map(d -> d.getCategory()).distinct().collect(Collectors.toList());

        List<DocumentFiltered> res = new ArrayList<>();
        categories.stream().forEach(c -> {
            List<DocumentFiltered> docsInCategory = docs.stream()
                    .filter(d -> d.getCategory().equals(c)).collect(Collectors.toList());
            if(docsInCategory.size() > minSize) {
                res.addAll(docsInCategory);
            }
        });

        return res;
    }
    
    private static void doCategorize(DocumentService documentService, Properties properties) {
    	int crossValidationNumParam = Integer.parseInt(properties.getProperty("crossValidation.num"));
    	
    	SVM svm = new SVM(Double.valueOf(properties.getProperty("svm.C")), Double.valueOf(properties.getProperty("svm.eps")), 
				documentService.getDim(), SolverType.L1R_L2LOSS_SVC);
    	
    	EvaluationService ovaEvaluationService = new EvaluationService(); 
    	EvaluationService ovoEvaluationService = new EvaluationService(); 
    	
    	for (CategoryType categoryType : CategoryType.values()) {
    		logger.info("Kategoria: " + categoryType.toString()); 
    		
    		for (int i = 0; i < crossValidationNumParam; ++i) {
    			List<DocumentFiltered> trainingList = documentService.getDocumentToTrain(crossValidationNumParam, i); 
    			List<DocumentFiltered> validationList = documentService.getDocumentsToValidate(crossValidationNumParam, i);
    			List<Integer> expectedResult = getExpectedResult(categoryType, trainingList); 
    			
    			logger.info("Run #" + i + ", rozmiar zbioru trenującego: " + trainingList.size() + ", rozmiar zbioru walidacji: " + validationList.size());
    			
    			List<Integer> ovaResult = svm.clsOVA(trainingList, validationList);		 
    			List<Integer> ovoResult = svm.clsOVO(trainingList, validationList);
    			ovaEvaluationService.evaluate(expectedResult, ovaResult);
    			ovoEvaluationService.evaluate(expectedResult, ovoResult); 
    		}
    		List<DocumentFiltered> trainingList = documentService.getTrainingDocuments(); 
    		List<DocumentFiltered> testList = documentService.getTestDocuments(); 
    		List<Integer> expectedTrainingListLabels = getExpectedResult(categoryType, trainingList); 
    		
    		logger.info("Pełna kategoryzacja; rozmiar zbioru treningowego: " + trainingList.size() + ", rozmiar zbioru testowego: " + testList); 
    		
    		List<Integer> ovaResult = svm.clsOVA(trainingList, testList); 
			List<Integer> ovoResult = svm.clsOVO(trainingList, testList);
			
			ovaEvaluationService.evaluate(categoryType, expectedTrainingListLabels, ovaResult, testList.size());
			ovoEvaluationService.evaluate(categoryType, expectedTrainingListLabels, ovoResult, testList.size()); 
    	}
    }

    
	private static List<Integer> getExpectedResult(CategoryType ct, List<DocumentFiltered> trainingList) {
		List<Integer> res = new ArrayList<Integer>();
		for (DocumentFiltered doc : trainingList){
			if (ct.equals(doc.getCategory()))
				res.add(doc.getCategory()); 
		}
			
		return res; 
	}
}
