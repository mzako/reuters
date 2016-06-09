package pl.wedt.reuters.service;

import java.util.Arrays;
import java.util.Collection;
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
	
	private Map<CategoryType, Map<Integer, ErrorMatrix>> matrixMap; 
	
	public EvaluationService() {
		matrixMap = new HashMap<CategoryType, Map<Integer, ErrorMatrix>>();
		Arrays.stream(CategoryType.values()).forEach(categoryType -> {
			matrixMap.put(categoryType, new HashMap<Integer, ErrorMatrix>() ); 
		});
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

    /**
     * Uzupełnia macierze pomyłek dla każdej kategorii na liście expectedResult. 
     * @param categoryType
     * @param expectedResult
     * @param actualResult
     * @param categoryList 
     */
	public void evaluate(CategoryType categoryType, List<Integer> expectedResult, List<Integer> actualResult, List<Integer> categoryList) {
		evaluate(expectedResult, categoryList);
		
		categoryList.forEach(cat -> matrixMap.get(categoryType).put(cat, new ErrorMatrix()));
		
		for (int i = 0; i < expectedResult.size(); ++i) {
			Integer exp = expectedResult.get(i); 
			Integer act = actualResult.get(i); 
			
			if (exp == act) {
				matrixMap.get(categoryType).get(exp).incA();
			}
			else {
				matrixMap.get(categoryType).get(exp).incC();
				if (act != -1) 
					matrixMap.get(categoryType).get(act).incB();
			}
		}
		
		categoryList.forEach(cat -> matrixMap.get(categoryType).get(cat).setD(expectedResult.size())); 
	}
	
	public void evaluateAll() {
		logger.info("Mikrouśredniodne macierze pomyłek dla typów kategorii:");
		Map<CategoryType, ErrorMatrix> categoryTypeMatrix = new HashMap<CategoryType, ErrorMatrix>();
		for (CategoryType categoryType : CategoryType.values()) {
			categoryTypeMatrix.put(categoryType, calculateMicroAverageMatrix(categoryType));
			logger.info("CategoryType: " + categoryType); 
			logger.info(categoryTypeMatrix.get(categoryType));
		}
		ErrorMatrix fullErrorMatrix = calculateMicroAverageMatrix(categoryTypeMatrix.values()); 
		logger.info("Mikrouśredniona macierz pomyłek dla całego procesu kategoryzacji: "); 
		logger.info(fullErrorMatrix);
	}

	private ErrorMatrix calculateMicroAverageMatrix(CategoryType categoryType) {
		return calculateMicroAverageMatrix(matrixMap.get(categoryType).values()); 
	}


	private ErrorMatrix calculateMicroAverageMatrix(Collection<ErrorMatrix> matrixMapValues) {
		double avgA = 0, avgB = 0, avgC = 0, avgD = 0;
		for (ErrorMatrix em : matrixMapValues) {
			avgA += em.getA(); 
			avgB += em.getB(); 
			avgC += em.getC(); 
			avgD += em.getD();
		}
		double catNum = matrixMapValues.size(); 
		return new ErrorMatrix(avgA/catNum, avgB/catNum, avgC/catNum, avgD/catNum); 
	}

	
//	public void evaluate(CategoryType ct, List<Integer> originalLabels, List<Integer> classificationLabels) {
//		evaluate(originalLabels, classificationLabels);
////		calculateErrorMatrices(ct, originalLabels, classificationLabels);
//	}
//	
//	public void evaluateAll() {
//		ErrorMatrix microAvgMatrix = microAverage();  			
//		logger.info(microAvgMatrix.toString());
//	}
//
//	private ErrorMatrix microAverage() {
//		double avgA = 0, avgB = 0, avgC = 0, avgD = 0;
//		for (CategoryType cat : CategoryType.values()) {
//			ErrorMatrix em = matrixMap.get(cat); 
//			avgA += em.getA(); 
//			avgB += em.getB(); 
//			avgC += em.getC(); 
//			avgD += em.getD(); 
//		}
//		int catNum = CategoryType.values().length;
//		return new ErrorMatrix(avgA/catNum, avgB/catNum, avgC/catNum, avgD/catNum); 
//	}
//
//	private void calculateErrorMatrix(CategoryType categoryType, List<Integer> expectedLabels, List<Integer> classificationLabels) {
//		ErrorMatrix em = new ErrorMatrix(); 
//		for (int i = 0; i < expectedLabels.size(); ++i) {
//			Integer exp = expectedLabels.get(i); 
//			Integer clas = classificationLabels.get(i);
//			
//		}
//	}
//	
////	private void calculateErrorMatrices(CategoryType ct, List<Integer> originalLabels, List<Integer> classificationLabels) {
////		int a = 0; 
////		for (Integer classLabel : classificationLabels) {
////			if (originalLabels.contains(classLabel)) 
////				a++; 
////		}
////		ErrorMatrix em = new ErrorMatrix(); 
////		em.setParams(a, originalLabels.size(), classificationLabels.size(), categoryDocumentNum);
////		
////		logger.info(em.toString()); 
////		
////		categoryMatrixMap.put(ct, em);  
////	}
//	










}