
package fedaemonfinal.dao;


import fedaemonfinal.frms.frmMonitor;
import fedaemonfinal.infact.ArrayOfImpuesto;
import fedaemonfinal.infact.ArrayOfInfoAdicional;
import fedaemonfinal.infact.ArrayOfMotivo;
import fedaemonfinal.infact.AutorizarNotaDebito;
import fedaemonfinal.infact.CloudAutorizarComprobante;
import fedaemonfinal.infact.IcloudAutorizarComprobante;
import fedaemonfinal.infact.Impuesto;
import fedaemonfinal.infact.InfoAdicional;
import fedaemonfinal.infact.InfoNotaDebito;
import fedaemonfinal.infact.InfoTributaria;
import fedaemonfinal.infact.Motivo;
import fedaemonfinal.infact.ObjectFactory;
import fedaemonfinal.infact.Response;
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
import java.text.SimpleDateFormat;
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
public final class NotaDebitoDAO {
    
     protected frmMonitor MONITOR;
    
     public int consultarNotasDebitoPendientes(ConexionBD con)throws Exception{
        int result=0;
        String select="SELECT COUNT(*) FROM INVE_NCND_FE_DAT "
                + "WHERE CODDOC='05' AND NUME_AUTO_INVE_DOCU IS NULL AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
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
     
     public int consultarNotasDebitoPendientes(ConexionBD con,String coddoc) {
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
     
     public int enviarNotasDebito(ConexionBD con)throws Exception{
        
        int enviadas=0;
        ObjectFactory factory = null;
        //OJO que al consultar data de la base se recuperará info como estaba hasta el ultimo COMMIT ejecutado
        String select="SELECT COUNT(*) LINEAS,TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "FROM INVE_NCND_FE_DAT "
                + "WHERE CODDOC='05' AND NUME_AUTO_INVE_DOCU IS NULL AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                + "GROUP BY TOTALSINIMPUESTO,DESCUENTO,VALORMODIFICACION,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "ORDER BY FECHAEMISION ASC,SECUENCIAL ASC";
        Statement st=null;
        ResultSet rs=null;
        InfoTrib ND=null;
        ArrayList<InfoTrib> arrayInfoTrib=null;
        ArrayList<AutorizarNotaDebito> arrayAutorizarNotaDebito=null;
        int band=0;
        long start=0;
        long stop = 0;
        Response resp=null;
        
        try{
            factory = new ObjectFactory();
            arrayInfoTrib=new ArrayList<>();
            arrayAutorizarNotaDebito=new ArrayList<>();
            st= con.getCon().createStatement();
            rs=st.executeQuery(select);
            
        while(rs.next())
        {   
            ND=new InfoTrib();
            ND.setLineas(rs.getString("LINEAS"));
            ND.setEstab(rs.getString("ESTAB"));
            ND.setPtoEmi(rs.getString("PTOEMI"));
            ND.setSecuencial(rs.getString("SECUENCIAL"));
            ND.setTotalSinImpuesto(rs.getDouble("TOTALSINIMPUESTO"));
            ND.setTotalDescuento(rs.getDouble("DESCUENTO"));
            ND.setTotalModificacion(rs.getDouble("VALORMODIFICACION"));
            arrayInfoTrib.add(ND);
        }
        rs.close();
        st.close();
        
        for(int i=0;i<arrayInfoTrib.size();i++)
        {
            this.MONITOR.limpiaND();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arrayInfoTrib.size());
            this.MONITOR.setMensajeND("[info] - Registro #"+(i+1)+ " de "+arrayInfoTrib.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoNotaDebito info_nd=new InfoNotaDebito();
            ArrayOfMotivo array_motivo=new ArrayOfMotivo();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();
        band=0;
        try
        {
                 String filtro="SELECT * FROM INVE_NCND_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='05' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' ""SELECT * FROM INVE_NCND_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='05' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                        +" AND ESTAB="+arrayInfoTrib.get(i).getEstab()
                        +" AND PTOEMI="+arrayInfoTrib.get(i).getPtoEmi()
                        +" AND SECUENCIAL="+arrayInfoTrib.get(i).getSecuencial();
                 st=con.getCon().createStatement();
                 rs=st.executeQuery(filtro);
                 
            while(rs.next())
            {
                
                if(band==0)
                {

                    //===================== INFORMACION TRIBUTARIA ====================================
                    info_t.setAmbiente(Integer.parseInt(rs.getString("AMBIENTE")));
                    info_t.setCodDoc(rs.getString("CODDOC"));
                    info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                    info_t.setEstab(rs.getString("ESTAB"));
                    JAXBElement<String> mailCliente=factory.createInfoTributariaMailCliente(rs.getString("MAILCLIENTE"));
                    info_t.setMailCliente(mailCliente);
                    JAXBElement<String> nombreComercial=factory.createInfoTributariaNombreComercial(rs.getString("NOMBRECOMERCIAL"));
                    info_t.setNombreComercial(nombreComercial);
                    JAXBElement<String> origen=factory.createInfoTributariaOrigen(rs.getString("ORIGEN"));
                    info_t.setOrigen((JAXBElement<String>) (origen==null?"":origen));
                    info_t.setPtoEmi(rs.getString("PTOEMI"));
                    info_t.setRazonSocial(rs.getString("RAZONSOCIAL"));
                    info_t.setRuc(rs.getString("RUC"));
                    info_t.setSecuencial(rs.getString("SECUENCIAL"));
                    info_t.setTipoEmision(rs.getInt("TIPOEMISION"));
                    

                    //===================== INFORMACION NOTA DE DEBITO ========================================
                    info_nd.setCodDocModificado(rs.getString("CODDOCMODIFICADO"));
                    JAXBElement<String> contribuyenteEspecial=factory.createInfoNotaDebitoContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                    info_nd.setContribuyenteEspecial(contribuyenteEspecial);
                    JAXBElement<String> dirEstablecimiento=factory.createInfoNotaDebitoDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO"));
                    info_nd.setDirEstablecimiento(dirEstablecimiento);
                    info_nd.setFechaEmision(rs.getString("FECHAEMISION"));
                    info_nd.setFechaEmisionDocSustento(rs.getString("FECHAEMISIONDOCSUSTENTO"));
                    info_nd.setIdentificacionComprador(rs.getString("IDENTIFICACIONCOMPRADOR"));
                    
                        Impuesto imp=new Impuesto();
                        imp.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE_IMP")));
                        imp.setCodigo(rs.getInt("CODIGO_IMP"));
                        imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE_IMP"));
                        imp.setTarifa(rs.getInt("TARIFA_IMP"));
                        imp.setValor(rs.getBigDecimal("TOTALIVA"));
//                        imp.setValor(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE_IMP")*rs.getDouble("TARIFA_IMP")/100));

                        ArrayOfImpuesto array_of_impuesto=new ArrayOfImpuesto();
                        array_of_impuesto.getImpuesto().add(imp);
                    
                    info_nd.setImpuestos(array_of_impuesto);
                    info_nd.setNumDocModificado(rs.getString("NUMDOCMODIFICADO"));
                    JAXBElement<String> obligadoContabilidad=factory.createInfoNotaDebitoObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD"));
                    info_nd.setObligadoContabilidad(obligadoContabilidad);
                    info_nd.setRazonSocialComprador(rs.getString("RAZONSOCIALCOMPRADOR"));
                    JAXBElement<String> rise=factory.createInfoNotaDebitoRise(rs.getString("RISE"));
                    info_nd.setRise((JAXBElement<String>) (rise==null?"NO REGISTRADO":rise));
                    info_nd.setTipoIdentificacionComprador(rs.getString("TIPOIDENTIFICACIONCOMPRADOR"));
                    info_nd.setTotalSinImpuestos(BigDecimal.valueOf(rs.getDouble("TOTALSINIMPUESTO")));
                    info_nd.setValorTotal(BigDecimal.valueOf(rs.getDouble("VALORMODIFICACION")));

                    
                    //============================ INFORMACION ADICIONAL =====================================
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

                    InfoAdicional info_a7=new InfoAdicional();
                    nombre=factory.createInfoAdicionalNombre("MOTIVO");
                    info_a7.setNombre(nombre);
                    text=factory.createInfoAdicionalText(rs.getString("MOTIVO").toUpperCase().trim());
                    info_a7.setText((JAXBElement<String>) (text==null?"NO REGISTRADO":text));


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
                    if(rs.getString("MOTIVO")!=null)
                        array_info_a.getInfoAdicional().add(info_a7);
                }
                
                //=========================== MOTIVOS ======================================
                    Motivo motivo=new Motivo();
                    motivo.setRazon(rs.getString("MOTIVO"));
                    motivo.setValor(BigDecimal.valueOf(rs.getDouble("VALORMODIFICACION")));

                    array_motivo.getMotivo().add(motivo);
                
            }
        }
        catch(SQLException e){e.printStackTrace();}
        finally{
            rs.close();
            st.close();
        }
        
                AutorizarNotaDebito autoriza=new AutorizarNotaDebito();
                autoriza.setInfoTributaria( new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                autoriza.setInfoNotaDebito(new JAXBElement(new QName("http://tempuri.org/","infoNotaDebito"),JAXBElement.class,info_nd));
                autoriza.setMotivos(new JAXBElement(new QName("http://tempuri.org/","motivos"),JAXBElement.class,array_motivo));
                autoriza.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                System.out.println("[info] - Generando xml...");  
                this.MONITOR.setMensajeND("[info] - Generando xml...");
                Marshaller m;
                String rutaXml;

                JAXBElement<AutorizarNotaDebito> jaxb_autoriza=new JAXBElement(new QName(AutorizarNotaDebito.class.getSimpleName()),AutorizarNotaDebito.class,autoriza);
                JAXBContext jaxb_context3=JAXBContext.newInstance(AutorizarNotaDebito.class);
                m=jaxb_context3.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
                rutaXml=this.MONITOR.dir_nd+"AutorizarNotaDebito"+arrayInfoTrib.get(i).getEstab()+"-"+arrayInfoTrib.get(i).getPtoEmi()+"-"+arrayInfoTrib.get(i).getSecuencial()+".xml";
                m.marshal(jaxb_autoriza, new File (rutaXml)); 

                start=0;
                stop = 0;
                resp=null;
                 
                try{
                    resp=new Response();
//                System.out.println("=============================================");
                this.MONITOR.setMensajeND("[info] - No. Lineas : "+arrayInfoTrib.get(i).getLineas());
                System.out.println("[info] - Enviando petición de autorización al WS...");
                this.MONITOR.setMensajeND("[info] - Enviando petición de autorización al WS...");
                //obteniendo el tiempo inicial para el tiempo de espera estimado
                start = Calendar.getInstance().getTimeInMillis();
                //Instancia del servicio de INTEME
                //El objeto Response encapsula la información del documento autorizado o no autorizado
                resp=autorizarNotaDebito(info_t,info_nd,array_motivo,array_info_a); 
                //obteniendo el tiempo final para el tiempo de espera estimado
                stop = Calendar.getInstance().getTimeInMillis();
    //            java.util.Date d = new java.util.Date(stop-start);

                System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" seg");
                this.MONITOR.setMensajeND("[info] - Tiempo de respuesta: "+(stop-start)+" seg");       
                enviadas++;
                System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                this.MONITOR.setMensajeND("No. de autorización: "+resp.getAutorizacion().getValue()); 
                System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                this.MONITOR.setMensajeND("Clave de acceso: "+resp.getClaveAcceso().getValue()); 
                System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                this.MONITOR.setMensajeND("Fecha Autorización: "+resp.getFechaAutorizacion().getValue()); 
                System.out.println("Id. Error: "+resp.getIdError().getValue());
                this.MONITOR.setMensajeND("Id. Error: "+resp.getIdError().getValue()); 
                System.out.println("Origen: "+resp.getOrigen().getValue());
                this.MONITOR.setMensajeND("Origen: "+resp.getOrigen().getValue()); 
                System.out.println("Result: "+resp.getResult().getValue());
                this.MONITOR.setMensajeND("Result: "+resp.getResult().getValue()); 
                System.out.println("Result Data: "+resp.getResultData().getValue());
                this.MONITOR.setMensajeND("Result Data: "+resp.getResultData().getValue()); 

                    if(resp.getAutorizacion().getValue()!=null)
                    {

                        this.MONITOR.setMensajeND("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        //llamada del metodo para actualizar registro
                        int reg=actualizarND(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeND("[info] - Registros actualizados : "+reg); 
                    }
                this.MONITOR.setMensajeND("[info] - Registrando en el log...");
                System.out.println("[info] - Registrando en el log...");
                //llamada del metodo para el registro del log
                notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                this.MONITOR.setMensajeND("[info] - Evento capturado en el historial");
                System.out.println("[info] - Evento capturado en el historial"); 
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeND("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeND("[error] - Ha surgido un error\n"+ex.getMessage());
                    notificarError(con, ex.getMessage(),info_t,String.valueOf((stop-start)));
                
                }
                finally{
                    if(resp!=null)
                    {resp=null;}
                    continue;
                }

            }//FINAL DEL FOR
        }
        catch(Exception ex)
        {
            this.MONITOR.setMensajeND("[error] - Error general al enviar a autorizar");
            System.out.println("[error] - Error general al enviar a autorizar");
        }
        finally
        {
            this.MONITOR.setMensajeND("[info] - Cancelando envío...");
            System.out.println("[info] - Cancelando envío...");
        }
        
        
        return enviadas;
    }

     public void generarXML(AutorizarNotaDebito autoriza,String estab,String ptoEmi,String secuencial){

                Marshaller m=null;
                String rutaXml;
                JAXBElement<AutorizarNotaDebito> jaxb_autoriza=null;
                JAXBContext jaxbContext=null;
                try
                {
                    System.out.println("[info] - Generando xml...");  
                    this.MONITOR.setMensajeND("[info] - Generando xml...");
                    jaxb_autoriza=new JAXBElement(new QName(AutorizarNotaDebito.class.getSimpleName()),AutorizarNotaDebito.class,autoriza);
                    jaxbContext=JAXBContext.newInstance(AutorizarNotaDebito.class);
                    m=jaxbContext.createMarshaller();
                    m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
                    rutaXml=this.MONITOR.dir_nd+"AutorizarNotaDebito"+estab+"-"+ptoEmi+"-"+secuencial+".xml";
                    m.marshal(jaxb_autoriza, new File (rutaXml)); 

                    System.out.println("[info] - xml generado "+rutaXml);  
                    this.MONITOR.setMensajeND("[info] - xml generado "+rutaXml);
                }
                catch(JAXBException ex)
                {
                    System.out.println("[error] - error al generar xml");  
                    this.MONITOR.setMensajeND("[error] - error al generar xml");}
                finally{}
     }
     
     private int actualizarND(ConexionBD con,Response resp,InfoTributaria info) {

         int result=0;
         String update="UPDATE INVE_NCND_FE_DAT SET NUME_AUTO_INVE_DOCU=? "
                 +" WHERE CODDOC='05' AND AMBIENTE=2 "
//                 + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                 + "AND ESTAB="+info.getEstab()+" AND PTOEMI="+info.getPtoEmi()+" AND SECUENCIAL="+info.getSecuencial();
         PreparedStatement ps=null;
         try
         {
             ps=con.getCon().prepareStatement(update);
             ps.setString(1, resp.getAutorizacion().getValue());
             result=ps.executeUpdate();
         }
         catch(SQLException sqle)
        {
            this.MONITOR.setMensajeND("[error] - Error al actualizar registros");
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
        ps.setInt(5,2);

        int n=ps.executeUpdate();
        ps.close();
        return n;

    }
     
     public int cambiaEstado(ConexionBD con,String estado,int atendiendo)throws SQLException, UnknownHostException{
    
        String update="UPDATE INVE_INFO_FE_DAT SET ESTATUS=?,USUARIO_ACT=?,ULT_EJECUCION=?,HOST_ACT=?,ATENDIENDO=? WHERE NOMBRE='HILO NOTAS DEBITO'";
        
        PreparedStatement ps = con.getCon().prepareStatement(update);
        ps.setString(1, estado);
        ps.setString(2, System.getProperty("user.name"));
        
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
//        String cad=now.getYear()+"/"+now.getMonth()+"/"+now.getDay();
        SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy");
//        System.out.println(f.format(now));
        
        ps.setDate(3,new java.sql.Date(now.getYear(), now.getMonth(), now.getDay()));
        InetAddress localHost = InetAddress.getLocalHost();
        ps.setString(4,localHost.getHostName());
        ps.setInt(5, atendiendo);
        
        int result=ps.executeUpdate();
        ps.close();
    return result;
    }

    private static Response autorizarNotaDebito(InfoTributaria infoTributaria, InfoNotaDebito infoNotaDebito, ArrayOfMotivo motivos,ArrayOfInfoAdicional infoAdicional) {
        Response respuesta = null;
        CloudAutorizarComprobante service = new CloudAutorizarComprobante();
        IcloudAutorizarComprobante port = service.getBasicHttpBindingIcloudAutorizarComprobante();
        try 
        {            
            respuesta = new Response();
            respuesta=port.autorizarNotaDebito(infoTributaria, infoNotaDebito, motivos, infoAdicional);
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
