
package fedaemonfinal.dao;


import fedaemonfinal.frms.frmMonitor;
import fedaemonfinal.infact.ArrayOfDetalleNC;
import fedaemonfinal.infact.ArrayOfImpuesto;
import fedaemonfinal.infact.ArrayOfInfoAdicional;
import fedaemonfinal.infact.ArrayOfTotalImpuesto;
import fedaemonfinal.infact.AutorizarNotaCredito;
import fedaemonfinal.infact.CloudAutorizarComprobante;
import fedaemonfinal.infact.DetalleNC;
import fedaemonfinal.infact.IcloudAutorizarComprobante;
import fedaemonfinal.infact.Impuesto;
import fedaemonfinal.infact.InfoAdicional;
import fedaemonfinal.infact.InfoNotaCredito;
import fedaemonfinal.infact.InfoTributaria;
import fedaemonfinal.infact.ObjectFactory;
import fedaemonfinal.infact.Response;
import fedaemonfinal.infact.TotalImpuesto;
import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.util.InfoTrib;
import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;


/**
 *
 * @author Mike
 */
public final class NotaCreditoDAO {
    
    protected frmMonitor MONITOR;
    
    public int consultarNotaCreditoPendientes(ConexionBD con)throws Exception{
    int result=0;
    String select="SELECT COUNT(*),ESTAB,PTOEMI,SECUENCIAL FROM INVE_NCND_FE_DAT "
            + "WHERE CODDOC='04' AND NUME_AUTO_INVE_DOCU IS NULL AND AMBIENTE=2 "
//            + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
            + "GROUP BY ESTAB,PTOEMI,SECUENCIAL";
        Statement st= con.getCon().createStatement();
        ResultSet rs=st.executeQuery(select);
        while(rs.next())
        {
        result++;
        }
    
     rs.close();
    return result;
    }
    
    public int consultarNotaCreditoPendientes(ConexionBD con,String coddoc) {
    int result=0;
    String sentencia="{call SP_FACTCONSULTAPENDIENTES(?,?)}";
    CallableStatement cs=null;
    try{
    cs=con.getCon().prepareCall(sentencia);
    
    cs.setString(1, coddoc);
    cs.registerOutParameter(2, java.sql.Types.NUMERIC);
    
    cs.execute();
    
    result=cs.getInt(2);
    }catch(SQLException ex){ex.printStackTrace();}
    finally{
        try{
         if(cs!=null)
            cs.close();
      }catch(SQLException se2){
      }
    }
    
    return result;
    
    }
    
    public int enviarNotasCredito(ConexionBD con){
        
        int enviadas=0;
        ObjectFactory factory = null;
        ArrayList<InfoTrib> arrayInfoTrib=null;
        ArrayList<AutorizarNotaCredito> arrayAutorizarNotaCredito =null;
        InfoTrib NC=null;
        //OJO que al consultar data de la base se recuperará info como estaba hasta el ultimo COMMIT ejecutado
        String select="SELECT COUNT(*) LINEAS,TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "FROM INVE_NCND_FE_DAT "
                + "WHERE CODDOC='04' AND NUME_AUTO_INVE_DOCU IS NULL AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                + "GROUP BY TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "ORDER BY FECHAEMISION ASC,SECUENCIAL ASC";
        Statement st= null;
        ResultSet rs=null;
        long start=0;
        long stop = 0;
        Response resp=null;
         int band=0;
        try{
            factory = new ObjectFactory();
            arrayInfoTrib=new ArrayList<>();
            arrayAutorizarNotaCredito=new ArrayList<>();
            st= con.getCon().createStatement();
            rs=st.executeQuery(select);
            while(rs.next())
            {   
                NC=new InfoTrib();
                NC.setLineas(rs.getString("LINEAS"));
                NC.setEstab(rs.getString("ESTAB"));
                NC.setPtoEmi(rs.getString("PTOEMI"));
                NC.setSecuencial(rs.getString("SECUENCIAL"));
                NC.setTotalSinImpuesto(rs.getDouble("TOTALSINIMPUESTO"));
                NC.setTotalDescuento(rs.getDouble("DESCUENTO"));
                NC.setTotalModificacion(rs.getDouble("VALORMODIFICACION"));
                arrayInfoTrib.add(NC);
            }
        rs.close();
        st.close();
        for(int i=0;i<arrayInfoTrib.size();i++)
        {
            this.MONITOR.limpiaNC();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arrayInfoTrib.size());
            this.MONITOR.setMensajeNC("[info] - Registro #"+(i+1)+ " de "+arrayInfoTrib.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoNotaCredito info_nc=new InfoNotaCredito();
            ArrayOfDetalleNC array_det=new ArrayOfDetalleNC();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();  
            ArrayOfTotalImpuesto array_total_imp=new ArrayOfTotalImpuesto();
            
            band=0;
            try{
                 String filtro="SELECT * FROM INVE_NCND_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='04' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'""SELECT * FROM INVE_NCND_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='04' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                        +" AND ESTAB="+arrayInfoTrib.get(i).getEstab()
                        +" AND PTOEMI="+arrayInfoTrib.get(i).getPtoEmi()
                        +" AND SECUENCIAL="+arrayInfoTrib.get(i).getSecuencial();
                 st=con.getCon().createStatement();
                 rs=st.executeQuery(filtro);
                 
               while(rs.next())
                {                   
                    if(band==0)
                    {
                        //============================ INFORMACION TRIBUTARIA =====================================
                        info_t.setAmbiente(Integer.parseInt(rs.getString("AMBIENTE")));
                        info_t.setCodDoc(rs.getString("CODDOC"));
                        info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                        info_t.setEstab(rs.getString("ESTAB"));
                        JAXBElement<String> mailCliente=factory.createInfoTributariaMailCliente(rs.getString("MAILCLIENTE"));
                        info_t.setMailCliente((JAXBElement<String>) (mailCliente==null?"":mailCliente));
                        JAXBElement<String> nombreComercial = factory.createInfoTributariaNombreComercial(rs.getString("NOMBRECOMERCIAL"));
                        info_t.setNombreComercial((JAXBElement<String>) (nombreComercial==null?" ":nombreComercial));
                        JAXBElement<String> origen=factory.createInfoTributariaOrigen(rs.getString("ORIGEN"));
                        info_t.setOrigen((JAXBElement<String>) (origen==null?"":origen));
                        info_t.setPtoEmi(rs.getString("PTOEMI"));
                        info_t.setRazonSocial(rs.getString("RAZONSOCIAL"));
                        info_t.setRuc(rs.getString("RUC"));
                        info_t.setSecuencial(rs.getString("SECUENCIAL"));
                        info_t.setTipoEmision(rs.getInt("TIPOEMISION"));

                        //================================ INFORMACION NOTA DE CREDITO =================================
                        info_nc.setCodDocModificado(rs.getString("CODDOCMODIFICADO"));
                        JAXBElement<String> contribuyenteEspecial=factory.createInfoNotaCreditoContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                        info_nc.setContribuyenteEspecial(contribuyenteEspecial);
                         JAXBElement<String> dirEstablecimiento=factory.createInfoNotaCreditoDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO"));
                        info_nc.setDirEstablecimiento(dirEstablecimiento);
                        info_nc.setFechaEmision(rs.getString("FECHAEMISION"));
//                        info_nc.setFechaEmisionDocSustento(f.format(rs.getDate("FECHAEMISIONDOCSUSTENTO")));
                        info_nc.setFechaEmisionDocSustento(rs.getString("FECHAEMISIONDOCSUSTENTO"));
                        info_nc.setIdentificacionComprador(rs.getString("IDENTIFICACIONCOMPRADOR"));
                        JAXBElement<String> moneda=factory.createInfoNotaCreditoMoneda(rs.getString("MONEDA"));
                        info_nc.setMoneda(moneda);
                        info_nc.setMotivo(rs.getString("MOTIVO"));
                        info_nc.setNumDocModificado(rs.getString("NUMDOCMODIFICADO"));
                        JAXBElement<String> obligadoContabilidad=factory.createInfoNotaCreditoObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD"));
                        info_nc.setObligadoContabilidad(obligadoContabilidad);
                        info_nc.setRazonSocialComprador(rs.getString("RAZONSOCIALCOMPRADOR"));
                        JAXBElement<String> rise=factory.createInfoNotaCreditoRise(rs.getString("RISE"));
                        info_nc.setRise((JAXBElement<String>) (rise==null?"":rise));
                        info_nc.setTipoIdentificacionComprador(rs.getString("TIPOIDENTIFICACIONCOMPRADOR"));
                        
                        //============================ TOTAL DE IMPUESTO DE LA NC =================================
                        TotalImpuesto total_imp=new TotalImpuesto();
                        total_imp.setBaseImponible(BigDecimal.valueOf(arrayInfoTrib.get(i).getTotalSinImpuesto()));
                        total_imp.setCodigo(rs.getInt("CODIGO"));
                        total_imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE"));
                        JAXBElement<String> tarifa=factory.createTotalImpuestoTarifa(rs.getString("TARIFA"));
                        total_imp.setTarifa(tarifa);
                        //OJO CON ESTE CAMPO
                        //REPRESENTA EL VALOR DEL TOTAL DE LOS IMPUESTOS,
                        //EL SRI RETORNA ERROR SI SE ENVIA CON MAS DE 2 DECIMALES

                        total_imp.setValor(BigDecimal.valueOf(rs.getDouble("TOTALIVA")));
                        array_total_imp.getTotalImpuesto().add(total_imp);
                        
                        info_nc.setTotalConImpuestos(array_total_imp);
                        info_nc.setTotalSinImpuestos(BigDecimal.valueOf(arrayInfoTrib.get(i).getTotalSinImpuesto()));
                        info_nc.setValorModificacion(BigDecimal.valueOf(arrayInfoTrib.get(i).getTotalModificacion()));

                        
                        //========================== INFORMACION ADICIONAL =======================================
                        InfoAdicional info_a1=new InfoAdicional();
                        JAXBElement<String> nombre=factory.createInfoAdicionalNombre("OBSERVACION");
                        info_a1.setNombre(nombre);
                        String observacion=rs.getString("OBSERVACION")==null?"NO REGISTRADO":rs.getString("OBSERVACION").toUpperCase().trim();
                        if(observacion!=null)
                        {
                            observacion=observacion.replace('Á','A');
                            observacion=observacion.replace('É','E');
                            observacion=observacion.replace('Í','I');
                            observacion=observacion.replace('Ó','O');
                            observacion=observacion.replace('Ú','U');
//                            observacion=observacion.replace(".", "");
//                            observacion=observacion.replace(",", "");
                            observacion=observacion.replace("\n", "");
                            observacion=observacion.replace("Ñ", "N");
                            observacion=observacion.replace("ñ", "n");
                        }
                        JAXBElement<String> text=factory.createInfoAdicionalText(observacion);
                        info_a1.setText(text);

                        InfoAdicional info_a2=new InfoAdicional();
                        nombre=factory.createInfoAdicionalNombre("CONTACTO");
                        info_a2.setNombre(nombre);
                        String contacto=rs.getString("CONTACTO")==null?"NO REGISTRADO":rs.getString("CONTACTO").toUpperCase().trim();
                        if(contacto!=null)
                        {
                            contacto=contacto.replace('Á','A');
                            contacto=contacto.replace('É','E');
                            contacto=contacto.replace('Í','I');
                            contacto=contacto.replace('Ó','O');
                            contacto=contacto.replace('Ú','U');
                            contacto=contacto.replace(".", "");
                        }
                        text=factory.createInfoAdicionalText(contacto);
                        info_a2.setText(text);
                        
                        InfoAdicional info_a3=new InfoAdicional();
                        nombre=factory.createInfoAdicionalNombre("DIRECCION");
                        info_a3.setNombre(nombre);
                        text=factory.createInfoAdicionalText(rs.getString("DIRECCION").toUpperCase().trim());
                        info_a3.setText((JAXBElement<String>) (text==null?"NO REGISTRADO":text));

                        InfoAdicional info_a4=new InfoAdicional();
                        nombre=factory.createInfoAdicionalNombre("EMAIL");
                        info_a4.setNombre(nombre);
                        text=factory.createInfoAdicionalText(rs.getString("MAILCLIENTE"));
                        info_a4.setText((JAXBElement<String>) (text==null?"NO REGISTRADO":text));

                        InfoAdicional info_a5=new InfoAdicional();
                        nombre=factory.createInfoAdicionalNombre("FONO");
                        info_a5.setNombre(nombre);
                        String fono=rs.getString("FONO")==null?"NO REGISTRADO":rs.getString("FONO").trim();
                        if(fono!=null)
                        {
                            fono=fono.replace("(","");
                            fono=fono.replace(")","");
                        }
                        text=factory.createInfoAdicionalText(fono);
                        info_a5.setText(text);

                        InfoAdicional info_a6=new InfoAdicional();
                        nombre=factory.createInfoAdicionalNombre("FONO_ESTAB");
                        info_a6.setNombre(nombre);
                        text=factory.createInfoAdicionalText(rs.getString("FONO_ESTAB").trim());
                        info_a6.setText((JAXBElement<String>) (text==null?"NO REGISTRADO":text));


                        if(rs.getString("OBSERVACION")!=null)
                            array_info_a.getInfoAdicional().add(info_a1);
                        if(rs.getString("CONTACTO")!=null)
                            array_info_a.getInfoAdicional().add(info_a2);
                        if(rs.getString("DIRECCION")!=null)
                            array_info_a.getInfoAdicional().add(info_a3);
                        if(rs.getString("MAILCLIENTE")!=null)
                            array_info_a.getInfoAdicional().add(info_a4);
                        if(rs.getString("FONO")!=null)
                            array_info_a.getInfoAdicional().add(info_a5);
                        if(rs.getString("FONO_ESTAB")!=null)
                            array_info_a.getInfoAdicional().add(info_a6);
                    }
                    
                    //============================DETALLE NOTA DE CREDITO=====================================
                    DetalleNC detalle=new DetalleNC();
                    detalle.setCantidad(BigDecimal.valueOf(rs.getDouble("CANTIDAD")));
                    JAXBElement<String> codigoAdicional=factory.createDetalleNCCodigoAdicional(rs.getString("CODIGOADICIONAL"));
                    detalle.setCodigoAdicional(codigoAdicional);
                    detalle.setCodigoInterno(rs.getString("CODIGOINTERNO"));
                    detalle.setDescripcion(rs.getString("DESCRIPCION"));
                    detalle.setDescuento(BigDecimal.valueOf(rs.getDouble("DESCUENTO")));

                   //            detalle.setDetallesAdicionales(null);

                    Impuesto imp=new Impuesto();
                    imp.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE_IMP")));
                    imp.setCodigo(rs.getInt("CODIGO_IMP"));
                    imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE_IMP"));
                    imp.setTarifa(rs.getInt("TARIFA_IMP"));
                    //EL SRI RETORNA ERROR SI SE ENVIA CON MAS DE 2 DECIMALES    
                    imp.setValor(BigDecimal.valueOf(rs.getDouble("VALOR_IMP")));

                    ArrayOfImpuesto array_imp=new ArrayOfImpuesto();
                    array_imp.getImpuesto().add(imp);

                    detalle.setImpuestos(array_imp);
                    detalle.setPrecioTotalSinImpuesto(BigDecimal.valueOf(rs.getDouble("PRECIOTOTALSINIMPUESTO")));
                    detalle.setPrecioUnitario(BigDecimal.valueOf(rs.getDouble("PRECIOUNITARIO")));


                    array_det.getDetalleNC().add(detalle);
                    band++;
                }    
                }
                catch(SQLException e)
                {
                    System.out.println("[error] - Error al empaquetar el documento. "+e.getMessage());
                    this.MONITOR.setMensajeNC("[error] - Error al empaquetar el documento. "+e.getMessage());
                }
                finally{
                    rs.close();
                    st.close();
                }
            
                        System.out.println("[info] - NOTA CREDITO "+arrayInfoTrib.get(i).getEstab()+"-"+arrayInfoTrib.get(i).getPtoEmi()+"-"+arrayInfoTrib.get(i).getSecuencial());
                        this.MONITOR.setMensajeNC("[info] - NOTA CREDITO "+arrayInfoTrib.get(i).getEstab()+"-"+arrayInfoTrib.get(i).getPtoEmi()+"-"+arrayInfoTrib.get(i).getSecuencial());
                        AutorizarNotaCredito autoriza=new AutorizarNotaCredito();
                        JAXBElement<InfoTributaria> jbInfoTributaria=factory.createAutorizarNotaCreditoInfoTributaria(info_t);
                        autoriza.setInfoTributaria( jbInfoTributaria);
                        JAXBElement<InfoNotaCredito> jbInfoNotaCredito=factory.createAutorizarNotaCreditoInfoNotaCredito(info_nc);
                        autoriza.setInfoNotaCredito(jbInfoNotaCredito);
                        JAXBElement<ArrayOfDetalleNC> jbArrayOfDetalleNC=factory.createAutorizarNotaCreditoDetalle(array_det);
                        autoriza.setDetalle(jbArrayOfDetalleNC);
                        JAXBElement<ArrayOfInfoAdicional> jbArrayOfInfoAdicional=factory.createAutorizarNotaCreditoInfoAdicional(array_info_a);
                        autoriza.setInfoAdicional(jbArrayOfInfoAdicional);

                        System.out.println("[info] - Generando xml...");
                        this.MONITOR.setMensajeNC("[info] - Generando xml...");
                        
                        Marshaller m;
                        String rutaXml=null;

                        JAXBElement<AutorizarNotaCredito> jaxb_autoriza=new JAXBElement(new QName(AutorizarNotaCredito.class.getSimpleName()),AutorizarNotaCredito.class,autoriza);
                        JAXBContext jaxb_context3=JAXBContext.newInstance(AutorizarNotaCredito.class);
                        m=jaxb_context3.createMarshaller();
                        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
                        rutaXml=this.MONITOR.dir_nc+"AutorizarNotaCredito"+arrayInfoTrib.get(i).getEstab()+"-"+arrayInfoTrib.get(i).getPtoEmi()+"-"+arrayInfoTrib.get(i).getSecuencial()+".xml";
                        m.marshal(jaxb_autoriza, new File (rutaXml));
                        System.out.println("[info] - xml generado "+rutaXml);  
                        this.MONITOR.setMensajeNC("[info] - xml generado "+rutaXml);
                        generarXML(autoriza,arrayInfoTrib.get(i).getEstab(),arrayInfoTrib.get(i).getPtoEmi(),arrayInfoTrib.get(i).getSecuencial());
                        
                        arrayAutorizarNotaCredito.add(autoriza);
                }//final del for        
                        
                start=0;
                stop = 0;
                resp=null;
                //Enviar documento empaquetado al webservice de SRI para autorizar
                for(int i=0;i<arrayAutorizarNotaCredito.size();i++){

                    System.out.println("[info] - No. Lineas : "+arrayInfoTrib.get(i).getLineas());
                    this.MONITOR.setMensajeNC("[info] - No. Líneas : "+arrayInfoTrib.get(i).getLineas());
                    System.out.println("[info] - Enviando petición de autorización al WS...");
                    this.MONITOR.setMensajeNC("[info] - Enviando petición de autorización al WS...");
                    //obteniendo el tiempo inicial para el tiempo de espera estimado
                    start = Calendar.getInstance().getTimeInMillis();
                    //Instancia del servicio de INTEME
                    //El objeto Response encapsula la información del documento autorizado o no autorizado
                    resp=autorizarNotaCredito(arrayAutorizarNotaCredito.get(i).getInfoTributaria().getValue()
                            ,arrayAutorizarNotaCredito.get(i).getInfoNotaCredito().getValue()
                            ,arrayAutorizarNotaCredito.get(i).getDetalle().getValue()
                            ,arrayAutorizarNotaCredito.get(i).getInfoAdicional().getValue());
                    //obteniendo el tiempo final para el tiempo de espera estimado
                    stop = Calendar.getInstance().getTimeInMillis();
                    
                    System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeNC("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                    
                    enviadas++;
                    System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                    this.MONITOR.setMensajeNC("No. de autorización: "+resp.getAutorizacion().getValue());
                    System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                    this.MONITOR.setMensajeNC("Clave de acceso: "+resp.getClaveAcceso().getValue());
                    System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                    this.MONITOR.setMensajeNC("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                    System.out.println("Id. Error: "+resp.getIdError().getValue());
                    this.MONITOR.setMensajeNC("Id. Error: "+resp.getIdError().getValue());
                    System.out.println("Origen: "+resp.getOrigen().getValue());
                    this.MONITOR.setMensajeNC("Origen: "+resp.getOrigen().getValue());
                    System.out.println("Result: "+resp.getResult().getValue());
                    this.MONITOR.setMensajeNC("Result: "+resp.getResult().getValue());
                    System.out.println("Result Data: "+resp.getResultData().getValue());
                    this.MONITOR.setMensajeNC("Result Data: "+resp.getResultData().getValue());

                    if(resp.getAutorizacion().getValue()!=null)
                    {
                        
                        this.MONITOR.setMensajeNC("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        //llamada del metodo para actualizar registro
                        int reg=actualizarNC(con, resp,arrayAutorizarNotaCredito.get(i).getInfoTributaria().getValue());
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeNC("[info] - Registros actualizados : "+reg);
                         
                    }
                    this.MONITOR.setMensajeNC("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                    //llamada del metodo para el registro del log
                    notificarResultado(con, resp,arrayAutorizarNotaCredito.get(i).getInfoTributaria().getValue(),String.valueOf((stop-start)));
                    this.MONITOR.setMensajeNC("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial");
                    
                }//final del FOR de envío
        }
        catch(Exception ex)
        {
            this.MONITOR.setMensajeNC("[error] - Error general al enviar a autorizar");
            System.out.println("[error] - Error general al enviar a autorizar");
        }
        finally
        {
            this.MONITOR.setMensajeNC("[info] - Cancelando envío...");
            System.out.println("[info] - Cancelando envío...");
        }
        return enviadas;
    }
    
    public void generarXML(AutorizarNotaCredito autorizar,String estab, String ptoEmi,String secuencial){
        Marshaller m=null;
        String rutaXml=null;
        JAXBElement<AutorizarNotaCredito> jaxb_autoriza=null;
        JAXBContext jaxbContext=null;
        try{
            System.out.println("[info] - Generando xml...");  
            this.MONITOR.setMensajeNC("[info] - Generando xml...");
 
            jaxb_autoriza=new JAXBElement(new QName(AutorizarNotaCredito.class.getSimpleName()),AutorizarNotaCredito.class,autorizar);
            jaxbContext=JAXBContext.newInstance(AutorizarNotaCredito.class);
            m=jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            rutaXml=this.MONITOR.dir_nc+"AutorizarFactura"+estab+"-"+ptoEmi+"-"+secuencial+".xml";
            m.marshal(jaxb_autoriza, new File (rutaXml)); 

            System.out.println("[info] - xml generado "+rutaXml);  
            this.MONITOR.setMensajeNC("[info] - xml generado "+rutaXml);
    
    }
    catch(JAXBException ex){
        System.out.println("[error] - Error al generar xml");  
            this.MONITOR.setMensajeNC("[error] - Error al generar xml");}
    finally{}
        
        
        
    
    }
    
    private int actualizarNC(ConexionBD con,Response resp,InfoTributaria info) {

        int result=0;
        String update="UPDATE INVE_NCND_FE_DAT SET NUME_AUTO_INVE_DOCU=? "
                + "WHERE CODDOC='04' AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                + "AND ESTAB="+info.getEstab()+" AND PTOEMI="+info.getPtoEmi()+" AND SECUENCIAL="+info.getSecuencial() ;
        PreparedStatement ps=null;
        try{
        ps=con.getCon().prepareStatement(update);
        ps.setString(1,resp.getAutorizacion().getValue());
        result=ps.executeUpdate();
        
        }
        catch(SQLException ex)
        {
            this.MONITOR.setMensajeNC("[error] - Error al actualizar registros");
            System.out.println("[error] - Error al actualizar registros");
        }
        finally
        {
              if(ps!=null)
            {
                try
                {
                    ps.close();
                }
                catch(SQLException e){
                System.out.println("[error] - Error al cerrar PreparedStatement");}
            }  
        }
        return result;
    }
    
    private int notificarResultado(ConexionBD con,Response resp,InfoTributaria info,String ts){
        int n=0;
        String insert="INSERT INTO INVE_LOG_FE_DAT(COD_DOC,NUM_DOC,NUM_AUTORIZACION,CLAVE_ACCESO,MENSAJE_DEVUELTO,TIEMPO_RESPUESTA,AMBIENTE)"+
            "VALUES(?,?,?,?,?,?,?)";
        PreparedStatement ps=null;
        
        try{
        ps=con.getCon().prepareStatement(insert);
        
        ps.setString(1,info.getCodDoc());
        ps.setString(2,info.getEstab()+"-"+info.getPtoEmi()+"-"+info.getSecuencial());
        ps.setString(3,resp.getAutorizacion().getValue());
        ps.setString(4,resp.getClaveAcceso().getValue());
//        ps.setDate(5, new java.sql.Date( new java.util.Date().getTime()));
        ps.setString(5,resp.getResult().getValue()+"\n"+resp.getResultData().getValue());
        ps.setString(6,ts);
        ps.setInt(7,2);
        
//        String sp="CALL MAIL_FILES(administrador.tevcol,michael.beltran@tevcol.com.ec,?,null,null,null,null)";
//        CallableStatement call=con.getCon().prepareCall(sp);
//        String  mensaje="Se ha generado Autorización del SRI:\nNo. Autorización: "+resp.getAutorizacion().getValue()
//                +"\nClave de Acceso: "+resp.getClaveAcceso().getValue()
//                +"\nFecha Autorización: "+resp.getFechaAutorizacion().getValue()
//                +"\nResult: "+resp.getResult().getValue()
//                +"\nResult Data: "+resp.getResultData().getValue();
//        
//        call.setString(1,mensaje);
//  
//        int n=call.executeUpdate();
        n=ps.executeUpdate();
        
        }
        catch(SQLException sqle)
        {
            this.MONITOR.setMensajeNC("[error] - Error al insertar notificación en el log");
            System.out.println("[error] - Error al insertar notificación en el log");
        }
        finally
        {
            if(ps!=null)
            {
                try
                {
                    ps.close();
                }
                catch(SQLException e){
                System.out.println("[error] - Error al cerrar PreparedStatement");}
            }
        }
        return n;
    }
    
    private int notificarError(ConexionBD con, String ex,InfoTributaria info,String ts)throws SQLException{
    String insert="INSERT INTO INVE_LOG_FE_DAT(COD_DOC,NUM_DOC,MENSAJE_DEVUELTO,TIEMPO_RESPUESTA,AMBIENTE)"+
            "VALUES(?,?,?,?,?)";
        PreparedStatement ps=con.getCon().prepareStatement(insert);
        
        ps.setString(1,info.getCodDoc());
        ps.setString(2,info.getEstab()+"-"+info.getPtoEmi()+"-"+info.getSecuencial());
        ps.setString(3,ex);
        ps.setString(4,ts);

        int n=ps.executeUpdate();
        ps.close();
        return n;

    }
    
    public int cambiaEstado(ConexionBD con,String estado,int atendiendo){
    
        int result=0;
        String update="UPDATE INVE_INFO_FE_DAT SET ESTATUS=?,USUARIO_ACT=?,ULT_EJECUCION=?,HOST_ACT=?,ATENDIENDO=? WHERE NOMBRE='HILO NOTAS CREDITO'";
        PreparedStatement ps=null;
        Calendar calendar =null;
        InetAddress localHost =null;
        try
        {
            ps = con.getCon().prepareStatement(update);
            ps.setString(1, estado);
            ps.setString(2, System.getProperty("user.name"));

            calendar= Calendar.getInstance();
            java.util.Date now = calendar.getTime();
            ps.setDate(3,new java.sql.Date(now.getYear(), now.getMonth(), now.getDay()));
            localHost = InetAddress.getLocalHost();
            ps.setString(4,localHost.getHostName());
            ps.setInt(5, atendiendo);

            result=ps.executeUpdate();
        }
        catch(SQLException sqle)
        {
            System.out.println("[error] - Error al actualizar estado del proceso");
        }
        catch(UnknownHostException uhe)
        {
            System.out.println("[error] - Error al recuperar InetAddress");
        }
        finally
        {
            if(ps!=null)
            {
                try
                {
                    ps.close();
                }
                catch(SQLException e){
                System.out.println("[error] - Error al cerrar PreparedStatement");}
            }
        }
        
    return result;
    }

    private static Response autorizarNotaCredito(InfoTributaria infoTributaria, InfoNotaCredito infoNotaCredito, ArrayOfDetalleNC detalle, ArrayOfInfoAdicional infoAdicional) {
        Response respuesta = null;
        CloudAutorizarComprobante service = new CloudAutorizarComprobante();
        IcloudAutorizarComprobante port = service.getBasicHttpBindingIcloudAutorizarComprobante();
        try 
        {            
            respuesta = new Response();
            respuesta=port.autorizarNotaCredito(infoTributaria, infoNotaCredito, detalle, infoAdicional);
            } 
        catch (Exception e) 
        {
            System.err.println("Error al invocar: " + e.getMessage());
        }
        finally
        {
            service = null;
            port = null;      
        }        
        return respuesta;
    }
    
    public frmMonitor getMONITOR() {
        return MONITOR;
    }

    public void setMONITOR(frmMonitor MONITOR) {
        this.MONITOR = MONITOR;
    }

    
}
