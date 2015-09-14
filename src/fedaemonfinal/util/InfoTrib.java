
package fedaemonfinal.util;

/**
 *
 * @author Mike
 */
public final class InfoTrib {
    
    protected String estab;
    protected String lineas;
    protected String secuencial;
    protected String ptoEmi;
    protected Double importeTotal;
    protected Double totalSinImpuesto;
    protected Double totalDescuento;
    protected Double totalModificacion;
    
    public InfoTrib(){}
    
    public String getEstab() {
        return estab;
    }

    public void setEstab(String estab) {
        this.estab = estab;
    }

    public String getSecuencial() {
        return secuencial;
    }

    public void setSecuencial(String secuencial) {
        this.secuencial = secuencial;
    }

    public String getPtoEmi() {
        return ptoEmi;
    }

    public void setPtoEmi(String ptoEmi) {
        this.ptoEmi = ptoEmi;
    }

    public Double getImporteTotal() {
        return importeTotal;
    }

    public void setImporteTotal(Double importeTotal) {
        this.importeTotal = importeTotal;
    }

    public Double getTotalSinImpuesto() {
        return totalSinImpuesto;
    }

    public void setTotalSinImpuesto(Double totalSinImpuesto) {
        this.totalSinImpuesto = totalSinImpuesto;
    }

    public Double getTotalDescuento() {
        return totalDescuento;
    }

    public void setTotalDescuento(Double totalDescuento) {
        this.totalDescuento = totalDescuento;
    }

    public Double getTotalModificacion() {
        return totalModificacion;
    }

    public void setTotalModificacion(Double totalModificacion) {
        this.totalModificacion = totalModificacion;
    }

    /**
     * @return the lineas
     */
    public String getLineas() {
        return lineas;
    }

    /**
     * @param lineas the lineas to set
     */
    public void setLineas(String lineas) {
        this.lineas = lineas;
    }
    
}
