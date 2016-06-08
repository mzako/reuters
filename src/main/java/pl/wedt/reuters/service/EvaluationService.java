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
	
	public void evaluate(List<Object> documents) {
		Map<CategoryType, ErrorMatrix> map = getErrorMatrices(documents);
		ErrorMatrix microAvgMatrix = microAverage(map);  			
		
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

	private Map<CategoryType, ErrorMatrix> getErrorMatrices(List<Object> documents) {
		Map<CategoryType, ErrorMatrix> map = new HashMap<CategoryType, ErrorMatrix>(); 
		
//		for (Object d : documents) {
//			for (CategoryType c : CategoryType.values()) {
//				if (/*d należy do c*/) {
//					if (/*d zakwalifikowany do c*/) {
//						map.get(c).incA();
//					}
//					else {
//						map.get(c).incC();
//					}
//				}
//				else {  // d nie należy do c
//					if (/*d zakwalifikowany do c*/) {
//						map.get(c).incB();
//					}
//					else {
//						map.get(c).incD();
//					}
//				}
//			}
//		}
			
		return map; 
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