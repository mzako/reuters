package pl.wedt.reuters.model;

import java.util.Arrays;

/**
 * @author Michał Żakowski
 *
 * Klasa reprezentująca dokument po przetworzeniu, w postaci wektora częstości słów
 */
public class DocumentFiltered {
    private Integer category;
    private double vector[];
    private int featurePosition[];

    public Integer getCategory() {
        return category;
    }

    public double[] getVector() {
        return vector;
    }

    public DocumentFiltered(Integer category, double[] vector, int featurePosition[]) {
        this.category = category;
        this.vector = vector;
        this.featurePosition = featurePosition;
    }

    public int[] getFeaturePosition() {
        return featurePosition;
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DocumentFiltered [category=");
		builder.append(category);
		builder.append(", vector=");
		builder.append(Arrays.toString(vector));
		builder.append(", featurePosition=");
		builder.append(Arrays.toString(featurePosition));
		builder.append("]");
		return builder.toString();
	}
}
