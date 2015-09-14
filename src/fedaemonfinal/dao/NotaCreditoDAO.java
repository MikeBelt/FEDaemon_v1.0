
package fedaemonfinal.dao;

import fedaemonfinal.ArrayOfDetalleNC;
import fedaemonfinal.ArrayOfImpuesto;
import fedaemonfinal.ArrayOfInfoAdicional;
import fedaemonfinal.ArrayOfTotalImpuesto;
import fedaemonfinal.AutorizarNotaCredito;
import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.DetalleNC;
import fedaemonfinal.Impuesto;
import fedaemonfinal.InfoAdicional;
import fedaemonfinal.InfoNotaCredito;
import fedaemonfinal.util.InfoTrib;
import fedaemonfinal.InfoTributaria;
import fedaemonfinal.Response;
import fedaemonfinal.TotalImpuesto;
import fedaemonfinal.frms.frmMonitor;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
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
    
    public int enviarNotasCredito(ConexionBD con)throws Exception{
        
        int enviadas=0;
        ArrayList<InfoTrib> arr=new ArrayList<>();
        InfoTrib NC=null;
        //OJO que al consultar data de la base se recuperará info como estaba hasta el ultimo COMMIT ejecutado
        String select="SELECT COUNT(*) LINEAS,TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "FROM INVE_NCND_FE_DAT "
                + "WHERE CODDOC='04' AND NUME_AUTO_INVE_DOCU IS NULL AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                + "GROUP BY TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "ORDER BY FECHAEMISION ASC,SECUENCIAL ASC";
        Statement st= con.getCon().createStatement();
//        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        ResultSet rs=st.executeQuery(select);
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
            arr.add(NC);
        }
        rs.close();
        st.close();
        for(int i=0;i<arr.size();i++)
        {
            this.MONITOR.limpiaNC();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arr.size());
            this.MONITOR.setMensajeNC("[info] - Registro #"+(i+1)+ " de "+arr.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoNotaCredito info_nc=new InfoNotaCredito();
            ArrayOfDetalleNC array_det=new ArrayOfDetalleNC();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();  
            ArrayOfTotalImpuesto array_total_imp=new ArrayOfTotalImpuesto();
            
            int band=0;
            try{
                 String filtro="SELECT * FROM INVE_NCND_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='04' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                        +" AND ESTAB="+arr.get(i).getEstab()
                        +" AND PTOEMI="+arr.get(i).getPtoEmi()
                        +" AND SECUENCIAL="+arr.get(i).getSecuencial();
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
                        info_t.setMailCliente(rs.getString("MAILCLIENTE")==null?"":rs.getString("MAILCLIENTE"));
                        info_t.setNombreComercial(rs.getString("NOMBRECOMERCIAL"));
                        info_t.setOrigen(rs.getString("ORIGEN")==null?"":rs.getString("ORIGEN"));
                        info_t.setPtoEmi(rs.getString("PTOEMI"));
                        info_t.setRazonSocial(rs.getString("RAZONSOCIAL"));
                        if(rs.getString("RUC").length()<13)
                            info_t.setRuc("0"+rs.getString("RUC"));
                        else
                            info_t.setRuc(rs.getString("RUC"));
                        info_t.setSecuencial(rs.getString("SECUENCIAL"));
                        info_t.setTipoEmision(rs.getInt("TIPOEMISION"));

                        //================================ INFORMACION NOTA DE CREDITO =================================
                        info_nc.setCodDocModificado(rs.getString("CODDOCMODIFICADO"));
                        info_nc.setContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                        info_nc.setDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO"));
//                        java.util.Date fech=new Date(rs.getDate("FECHAEMISION").getYear(),rs.getDate("FECHAEMISION").getMonth(),rs.getDate("FECHAEMISION").getDay());
//                        java.util.Date fech=new Date(rs.getString("FECHAEMISION"));
//                        info_nc.setFechaEmision(f.format(fech));
                        info_nc.setFechaEmision(rs.getString("FECHAEMISION"));
//                        info_nc.setFechaEmisionDocSustento(f.format(rs.getDate("FECHAEMISIONDOCSUSTENTO")));
                        info_nc.setFechaEmisionDocSustento(rs.getString("FECHAEMISIONDOCSUSTENTO"));
                        info_nc.setIdentificacionComprador(rs.getString("IDENTIFICACIONCOMPRADOR"));
                        info_nc.setMoneda(rs.getString("MONEDA"));
                        info_nc.setMotivo(rs.getString("MOTIVO"));
                        info_nc.setNumDocModificado(rs.getString("NUMDOCMODIFICADO"));
                        info_nc.setObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD"));
                        info_nc.setRazonSocialComprador(rs.getString("RAZONSOCIALCOMPRADOR"));
                        info_nc.setRise(rs.getString("RISE")==null?"":rs.getString("RISE"));
                        info_nc.setTipoIdentificacionComprador(rs.getString("TIPOIDENTIFICACIONCOMPRADOR"));
                        
                        //============================ TOTAL DE IMPUESTO DE LA NC =================================
                        TotalImpuesto total_imp=new TotalImpuesto();
                        total_imp.setBaseImponible(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));
                        total_imp.setCodigo(rs.getInt("CODIGO"));
                        total_imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE"));
                        total_imp.setTarifa(rs.getString("TARIFA"));
                        //OJO CON ESTE CAMPO
                        //REPRESENTA EL VALOR DEL TOTAL DE LOS IMPUESTOS,
                        //EL SRI RETORNA ERROR SI SE ENVIA CON MAS DE 2 DECIMALES
//                        Double valor_total_imp=arr.get(i).getTotalSinImpuesto()*rs.getDouble("TARIFA")/100;
//                        BigDecimal big=new BigDecimal(valor_total_imp);
//                        big=big.setScale(2,RoundingMode.HALF_UP);
                        
//                        total_imp.setValor(big);
                        total_imp.setValor(BigDecimal.valueOf(rs.getDouble("TOTALIVA")));
                        array_total_imp.getTotalImpuesto().add(total_imp);
                        
                        info_nc.setTotalConImpuestos(array_total_imp);
                        info_nc.setTotalSinImpuestos(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));
                        info_nc.setValorModificacion(BigDecimal.valueOf(arr.get(i).getTotalModificacion()));

                        
                        //========================== INFORMACION ADICIONAL =======================================
                        InfoAdicional info_a1=new InfoAdicional();
                        info_a1.setNombre("OBSERVACION");
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
                        info_a1.setText(observacion);

                        InfoAdicional info_a2=new InfoAdicional();
                        info_a2.setNombre("CONTACTO");
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
                        info_a2.setText(contacto);
                        
                        InfoAdicional info_a3=new InfoAdicional();
                        info_a3.setNombre("DIRECCION");
                        info_a3.setText(rs.getString("DIRECCION")==null?"NO REGISTRADO":rs.getString("DIRECCION").toUpperCase().trim());

                        InfoAdicional info_a4=new InfoAdicional();
                        info_a4.setNombre("EMAIL");
                        info_a4.setText(rs.getString("MAILCLIENTE")==null?"NO REGISTRADO":rs.getString("MAILCLIENTE"));

                        InfoAdicional info_a5=new InfoAdicional();
                        info_a5.setNombre("FONO");
                        String fono=rs.getString("FONO")==null?"NO REGISTRADO":rs.getString("FONO").trim();
                        if(fono!=null)
                        {
                            fono=fono.replace("(","");
                            fono=fono.replace(")","");
                        }
                        info_a5.setText(fono);

                        InfoAdicional info_a6=new InfoAdicional();
                        info_a6.setNombre("FONO_ESTAB");
                        info_a6.setText(rs.getString("FONO_ESTAB")==null?"NO REGISTRADO":rs.getString("FONO_ESTAB").trim());


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
                    detalle.setCodigoAdicional(rs.getString("CODIGOADICIONAL"));
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
//                        Double valor_imp=rs.getDouble("BASEIMPONIBLE_IMP")*rs.getDouble("TARIFA_IMP")/100;
//                        BigDecimal big=new BigDecimal(valor_imp);
//                        big=big.setScale(2,RoundingMode.HALF_UP);
//                    imp.setValor(big);    
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
                catch(SQLException e){e.printStackTrace();}
                finally{
                    rs.close();
                    st.close();
                }
            
                        System.out.println("[info] - NOTA CREDITO "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                        this.MONITOR.setMensajeNC("[info] - NOTA CREDITO "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                        AutorizarNotaCredito autoriza=new AutorizarNotaCredito();
                        autoriza.setInfoTributaria( new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                        autoriza.setInfoNotaCredito(new JAXBElement(new QName("http://tempuri.org/","infoNotaCredito"),JAXBElement.class,info_nc));
                        autoriza.setDetalle(new JAXBElement(new QName("http://tempuri.org/","detalle"),JAXBElement.class,array_det));
                        autoriza.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                        System.out.println("[info] - Generando xml...");
                        this.MONITOR.setMensajeNC("[info] - Generando xml...");
                        
                        Marshaller m;
                        String rutaXml=null;

                        JAXBElement<AutorizarNotaCredito> jaxb_autoriza=new JAXBElement(new QName(AutorizarNotaCredito.class.getSimpleName()),AutorizarNotaCredito.class,autoriza);
                        JAXBContext jaxb_context3=JAXBContext.newInstance(AutorizarNotaCredito.class);
                        m=jaxb_context3.createMarshaller();
                        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
                        rutaXml=this.MONITOR.dir_nc+"AutorizarNotaCredito"+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial()+".xml";
                        m.marshal(jaxb_autoriza, new File (rutaXml));
                        System.out.println("[info] - xml generado "+rutaXml);  
                this.MONITOR.setMensajeNC("[info] - xml generado "+rutaXml);
                        
                long start=0;
                long stop = 0;
                Response resp=null;
                 
                try{
                    resp=new Response();
//                    System.out.println("=============================================");
                    System.out.println("[info] - No. Lineas : "+arr.get(i).getLineas());
                    this.MONITOR.setMensajeNC("[info] - No. Líneas : "+arr.get(i).getLineas());
                    System.out.println("[info] - Enviando petición de autorización al WS...");
                    this.MONITOR.setMensajeNC("[info] - Enviando petición de autorización al WS...");
                    //obteniendo el tiempo inicial para el tiempo de espera estimado
                    start = Calendar.getInstance().getTimeInMillis();
                    //Instancia del servicio de INTEME
                    //El objeto Response encapsula la información del documento autorizado o no autorizado
                    resp=autorizarNotaCredito(info_t,info_nc,array_det,array_info_a);
                    //obteniendo el tiempo final para el tiempo de espera estimado
                    stop = Calendar.getInstance().getTimeInMillis();
//                    java.util.Date d = new java.util.Date(stop-start);
                    
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
                        try{ 
                        this.MONITOR.setMensajeNC("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        //llamada del metodo para actualizar registro
                        int reg=actualizarNC(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeNC("[info] - Registros actualizados : "+reg);
                         }
                        catch(SQLException ex)
                        {
                        this.MONITOR.setMensajeNC("[error] - Error al hacer la actualizacion de campos");
                        System.out.println("[error] - Error al hacer la actualizacion de campos");
                        }
                    }
                this.MONITOR.setMensajeNC("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                    //llamada del metodo para el registro del log
                notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                   this.MONITOR.setMensajeNC("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial"); 
                    
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeNC("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeNC("[error] - Ha surgido un error\n"+ex.getMessage());
                    notificarError(con, ex.getMessage(),info_t,String.valueOf((stop-start)));
                
                }
                finally{
                    if(resp!=null)
                    {resp=null;}
                    continue;
                }
                    
            }//FINAL DEL FOR

        return enviadas;
    }
    
    private int actualizarNC(ConexionBD con,Response resp,InfoTributaria info) throws SQLException{

        String update="UPDATE INVE_NCND_FE_DAT SET NUME_AUTO_INVE_DOCU=? "
                + "WHERE CODDOC='04' AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                + "AND ESTAB="+info.getEstab()+" AND PTOEMI="+info.getPtoEmi()+" AND SECUENCIAL="+info.getSecuencial() ;
        PreparedStatement ps=con.getCon().prepareStatement(update);
        ps.setString(1,resp.getAutorizacion().getValue());
        int result=ps.executeUpdate();
        ps.close();

        return result;
    }
    
    private int notificarResultado(ConexionBD con,Response resp,InfoTributaria info,String ts)throws SQLException{
        String insert="INSERT INTO INVE_LOG_FE_DAT(COD_DOC,NUM_DOC,NUM_AUTORIZACION,CLAVE_ACCESO,MENSAJE_DEVUELTO,TIEMPO_RESPUESTA,AMBIENTE)"+
            "VALUES(?,?,?,?,?,?,?)";
        PreparedStatement ps=con.getCon().prepareStatement(insert);
        
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
        int n=ps.executeUpdate();
        ps.close();
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
    
    public int cambiaEstado(ConexionBD con,String estado,int atendiendo)throws SQLException, UnknownHostException{
    
        String update="UPDATE INVE_INFO_FE_DAT SET ESTATUS=?,USUARIO_ACT=?,ULT_EJECUCION=?,HOST_ACT=?,ATENDIENDO=? WHERE NOMBRE='HILO NOTAS CREDITO'";
        
        PreparedStatement ps = con.getCon().prepareStatement(update);
        ps.setString(1, estado);
        ps.setString(2, System.getProperty("user.name"));
        
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
//        String cad=now.getYear()+"/"+now.getMonth()+"/"+now.getDay();
        
//        System.out.println(f.format(now));
        
        ps.setDate(3,new java.sql.Date(now.getYear(), now.getMonth(), now.getDay()));
        InetAddress localHost = InetAddress.getLocalHost();
        ps.setString(4,localHost.getHostName());
        ps.setInt(5, atendiendo);
        
        int result=ps.executeUpdate();
        ps.close();
    return result;
    }

    private static Response autorizarNotaCredito(fedaemonfinal.InfoTributaria infoTributaria, fedaemonfinal.InfoNotaCredito infoNotaCredito, fedaemonfinal.ArrayOfDetalleNC detalle, fedaemonfinal.ArrayOfInfoAdicional infoAdicional) {
        Response respuesta = null;
        fedaemonfinal.CloudAutorizarComprobante service = new fedaemonfinal.CloudAutorizarComprobante();
        fedaemonfinal.IcloudAutorizarComprobante port = service.getBasicHttpBindingIcloudAutorizarComprobante();
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
