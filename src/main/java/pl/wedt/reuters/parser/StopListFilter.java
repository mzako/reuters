package pl.wedt.reuters.parser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michał Żakowski
 *
 * Klasa filtrująca słowa ze stop listy
 */
public class StopListFilter {
    private Set<String> stopSet;

    public StopListFilter(Path stopListPath) {
        try {
            List<String> stopList = Files.readAllLines(stopListPath);
            stopSet = stopList.stream().collect(Collectors.toSet());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> filter(List<String> words) {
        List<String> result;
        result = words.stream().filter(i -> !stopSet.contains(i)).collect(Collectors.toList());

        return result;
    }
}
