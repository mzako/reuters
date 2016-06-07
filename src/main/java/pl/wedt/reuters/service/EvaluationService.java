package pl.wedt.reuters.service;

import org.apache.log4j.Logger;

import java.util.List;

/**
 * @author Michał Żakowski
 */
public class EvaluationService {
    private final Logger logger = Logger.getLogger(EvaluationService.class);

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
