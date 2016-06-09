package pl.wedt.reuters.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import pl.wedt.reuters.model.CategoryType;
import pl.wedt.reuters.model.ErrorMatrix;

/**
 * 
 * @author Anna Czajka
 *
 */
public class EvaluationService {
	
	private final Logger logger = Logger.getLogger(EvaluationService.class);
	
	Map<CategoryType, ErrorMatrix> categoryMatrixMap; 
	
	public EvaluationService() {
		categoryMatrixMap = new HashMap<CategoryType, ErrorMatrix>();
	}
	
	public void evaluate(CategoryType ct, List<Integer> originalLabels, List<Integer> classificationLabels, int documentNum) {
		evaluate(originalLabels, classificationLabels);
		
		calculateErrorMatrices(ct, originalLabels, classificationLabels, documentNum);
		ErrorMatrix microAvgMatrix = microAverage(categoryMatrixMap);  			
		
		logger.info(microAvgMatrix.toString());
	}

	private ErrorMatrix microAverage(Map<CategoryType, ErrorMatrix> map) {
		double avgA = 0, avgB = 0, avgC = 0, avgD = 0;
		for (CategoryType cat : CategoryType.values()) {
			ErrorMatrix em = map.get(cat); 
			avgA += em.getA(); 
			avgB += em.getB(); 
			avgC += em.getC(); 
			avgD += em.getD(); 
		}
		int catNum = CategoryType.values().length;
		return new ErrorMatrix(avgA/catNum, avgB/catNum, avgC/catNum, avgD/catNum); 
	}

	private void calculateErrorMatrices(CategoryType ct, List<Integer> originalLabels, List<Integer> classificationLabels, int categoryDocumentNum) {
		int a = 0; 
		for (Integer classLabel : classificationLabels) {
			if (originalLabels.contains(classLabel)) 
				a++; 
		}
		ErrorMatrix em = new ErrorMatrix(); 
		em.setParams(a, originalLabels.size(), classificationLabels.size(), categoryDocumentNum);
		
		categoryMatrixMap.put(ct, em);  
	}
	
    public void evaluate(List<Integer> originalLabels, List<Integer> classificationLabels) {
        int dontKnows = 0;
        int errors = 0;

        for(int i = 0; i < originalLabels.size(); i++) {
            if (classificationLabels.get(i) == -1) {
                dontKnows++;
            } else if (!classificationLabels.get(i).equals(originalLabels.get(i))) {
                errors++;
            }
        }
        logger.info("Poprawne: " + (1.0 - (double) (errors  + dontKnows) / originalLabels.size())
                + " Błędne: " + (double) errors  / originalLabels.size()
                + " Odrzucone: " + (double) dontKnows / originalLabels.size());
    }

}