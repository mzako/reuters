package pl.wedt.reuters.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * @author Michał Żakowski
 *
 * Klasa udostępniająca kategorie
 */
public class Category {
    private final static String EXCHANGES_FILE_NAME = "all-exchanges-strings.lc.txt";
    private final static String ORGS_FILE_NAME = "all-orgs-strings.lc.txt";
    private final static String PEOPLE_FILE_NAME = "all-people-strings.lc.txt";
    private final static String PLACES_FILE_NAME = "all-places-strings.lc.txt";
    private final static String TOPICS_FILE_NAME = "all-topics-strings.lc.txt";

    public static Map<String, Integer> EXCHANGES;
    public static Map<String, Integer> ORGS;
    public static Map<String, Integer> PEOPLE;
    public static Map<String, Integer> PLACES;
    public static Map<String, Integer> TOPICS;

    public static void loadData(String path) throws IOException {
        EXCHANGES = new HashMap();
        ORGS = new HashMap();
        PEOPLE = new HashMap();
        PLACES = new HashMap();
        TOPICS = new HashMap();

        fill(path, EXCHANGES_FILE_NAME, EXCHANGES);
        fill(path, ORGS_FILE_NAME, ORGS);
        fill(path, PEOPLE_FILE_NAME, PEOPLE);
        fill(path, PLACES_FILE_NAME, PLACES);
        fill(path, TOPICS_FILE_NAME, TOPICS);
    }

    private static void fill(String path, String filename, Map<String, Integer> map) throws IOException {
        List<String> names = Files.readAllLines(Paths.get(path, filename));
        IntStream.range(0, names.size()).forEach(i -> map.put(names.get(i), i));
    }
}
