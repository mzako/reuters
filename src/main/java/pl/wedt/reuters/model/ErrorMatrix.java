package pl.wedt.reuters.model;

/**
 * 
 * @author Anna Czajka
 *
 */
public class ErrorMatrix {
	/*								faktycznie należy 			faktycznie nie należy						
	 * 
	 * wg klasyfikatora należy			   a							  b
	 * 
	 * wg klasyf. nie należy			   c							  d
	 * 
	 */
	
	private double a;
	private double b; 
	private double c; 
	private double d;
	
	public ErrorMatrix() {
		a = 0; 
		b = 0;
		c = 0; 
		d = 0; 
	}
	
	public ErrorMatrix(double a, double b, double c, double d) {
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
	}

	public void incA() {
		a++; 
	}
	
	public void incB() {
		b++; 
	}
	
	public void incC() {
		c++; 
	}
	
	public void incD() {
		d++; 
	}
	
	public void setD(int num) {
		d = num - this.b - this.c + this.a; 
	}
	
	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getC() {
		return c;
	}

	public double getD() {
		return d;
	}

	public double getPrecision() {  // precyzja, PR
		return a/(a+b); 
	}
	
	public double getRecall() {		// zupełność, kompletność, R
		return a/(a+c); 
	}
	
	public double getAccuracy() {	// dokładność, A
		return (a+d)/(a+b+c+d);
	}
	
	public double getFallout() { 	// zaszumienie, FO
		return b/(b+d);
	}

	/**
	 * Uzupełnia parametry macierzy na podstawie rozmiarów zbiorów. 
	 * @param a	rozmiar części wspólnej zbiorów: faktycznie należące do kategorii i zakwalifikowane do niej
	 * @param originalNum liczba faktycznie należących do kategorii
	 * @param classificationNum 
	 * @param categoryDocumentNum
	 */
	public void setParams(int a, int originalNum, int classificationNum, int categoryDocumentNum) {
		this.a = a; 													// część wspólna zbiorów
		this.b = classificationNum - a;
		this.c = originalNum - a; 
		this.d = categoryDocumentNum - this.b - this.c + this.a; 
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ErrorMatrix [a=");
		builder.append(a);
		builder.append(", b=");
		builder.append(b);
		builder.append(", c=");
		builder.append(c);
		builder.append(", d=");
		builder.append(d);
		builder.append(", PR=");
		builder.append(getPrecision());
		builder.append(", R=");
		builder.append(getRecall());
		builder.append(", A=");
		builder.append(getAccuracy());
		builder.append(", FO=");
		builder.append(getFallout());
		builder.append("]");
		return builder.toString();
	}

	
	
}
