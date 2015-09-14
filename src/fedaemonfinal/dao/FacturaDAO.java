
package fedaemonfinal.dao;

import fedaemonfinal.ArrayOfDetAdicional;
import fedaemonfinal.ArrayOfDetalle;
import fedaemonfinal.ArrayOfImpuesto;
import fedaemonfinal.ArrayOfInfoAdicional;
import fedaemonfinal.ArrayOfTotalImpuesto;
import fedaemonfinal.AutorizarFactura;
import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.DetAdicional;
import fedaemonfinal.Detalle;
import fedaemonfinal.Impuesto;
import fedaemonfinal.InfoAdicional;
import fedaemonfinal.InfoFactura;
import fedaemonfinal.util.InfoTrib;
import fedaemonfinal.InfoTributaria;
import fedaemonfinal.Response;
import fedaemonfinal.TotalImpuesto;
import fedaemonfinal.frms.frmMonitor;
import java.io.File;
import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.CallableStatement;
import java.sql.Date;
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
 * @author Michael Beltran
 */
public final class FacturaDAO {
    
    protected frmMonitor MONITOR;
    
    public int consultarFacturasPendientes(ConexionBD con)throws Exception{
    int result=0;
    String select="SELECT COUNT(*),ESTAB,PTOEMI,SECUENCIAL FROM INVE_DOCUMENTOS_FE_DAT "
            + "WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='01' AND AMBIENTE=2 "
//            + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
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
    
    public int consultarFacturasPendientes(ConexionBD con,String coddoc) {
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
    
    public int enviarFacturas(ConexionBD con)throws Exception{

        int enviadas=0;
        InfoTrib fra=null;
        ArrayList<InfoTrib> arr=new ArrayList<>();
        //OJO que al consultar data de la base se recuperará info como estaba hasta el ultimo COMMIT ejecutado
        String select="SELECT COUNT(*) LINEAS,TOTALSINIMPUESTOS,TOTALDESCUENTO,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "FROM INVE_DOCUMENTOS_FE_DAT "
                + "WHERE NUME_AUTO_INVE_DOCU IS NULL "
                + "AND CODDOC='01' "
                + "AND AMBIENTE=2"
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                + "GROUP BY TOTALSINIMPUESTOS,TOTALDESCUENTO,ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "ORDER BY FECHAEMISION ASC";
                
        Statement st= con.getCon().createStatement();
        ResultSet rs=st.executeQuery(select);
//        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        //Almacenando en el ArrayList los documentos que se van a enviar
        while(rs.next())
        {   
            fra=new InfoTrib();
            fra.setLineas(rs.getString("LINEAS"));
            fra.setEstab(rs.getString("ESTAB"));
            fra.setPtoEmi(rs.getString("PTOEMI"));
            fra.setSecuencial(rs.getString("SECUENCIAL"));
            fra.setTotalSinImpuesto(rs.getDouble("TOTALSINIMPUESTOS"));
            fra.setTotalDescuento(rs.getDouble("TOTALDESCUENTO"));
            arr.add(fra);
        }
        rs.close();
        st.close();
        
        //Recorriendo el arreglo que contiene documentos listos para enviar
            for(int i=0;i<arr.size();i++)
            {
//            if(arr.get(i).getSecuencial().equals("000000267"))     
//            {
                this.MONITOR.limpiaFacturas();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arr.size());
            this.MONITOR.setMensajeFacturas("[info] - Registro #"+(i+1)+ " de "+arr.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoFactura info_f=new InfoFactura();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();
            ArrayOfDetalle array_det=new ArrayOfDetalle();
            ArrayOfTotalImpuesto array_total_imp=new ArrayOfTotalImpuesto();
            
            
            //===========================PUEDEN HABER FACTURAS CON VARIOS DETALLES====================================

            int band=0;
            try{
                st=con.getCon().createStatement();
                 String filtro="SELECT * FROM INVE_DOCUMENTOS_FE_DAT WHERE NUME_AUTO_INVE_DOCU IS NULL AND CODDOC='01' AND AMBIENTE=2 "
//                         + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                        +" AND ESTAB="+arr.get(i).getEstab()
                        +" AND PTOEMI="+arr.get(i).getPtoEmi()
                        +" AND SECUENCIAL="+arr.get(i).getSecuencial();
                 rs=st.executeQuery(filtro);
               while(rs.next())
                {
                    
                    if(band==0)
                    {

                        //==================================== INFORMACION TRIBUTARIA ===========================
                        info_t.setAmbiente(rs.getInt("AMBIENTE"));
                        info_t.setCodDoc(rs.getString("CODDOC"));
                        info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                        info_t.setEstab(rs.getString("ESTAB"));
                        info_t.setMailCliente(rs.getString("MAILCLIENTE")==null?"":rs.getString("MAILCLIENTE"));
                        info_t.setNombreComercial(rs.getString("NOMBRECOMERCIAL")==null?" ":rs.getString("NOMBRECOMERCIAL"));
                        info_t.setOrigen(rs.getString("ORIGEN")==null?"":rs.getString("ORIGEN"));
                        info_t.setPtoEmi(rs.getString("PTOEMI"));
                        info_t.setRazonSocial(rs.getString("RAZONSOCIAL"));
                         if(rs.getString("RUC").length()<13)
                            info_t.setRuc("0"+rs.getString("RUC"));
                        else
                            info_t.setRuc(rs.getString("RUC"));
                        info_t.setSecuencial(rs.getString("SECUENCIAL"));
                        info_t.setTipoEmision(rs.getInt("TIPOEMISION"));
                        
                        
                        //====================================== INFORMACION DE FACTURA ===========================
                        
                        info_f.setContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                        info_f.setDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO").replace(",", " "));
//                        info_f.setFechaEmision(f.format(rs.getDate("FECHAEMISION")));
                        info_f.setFechaEmision(rs.getString("FECHAEMISION"));
//                        info_f.setGuiaRemision(String.valueOf(rs.getLong("GUIAREMISION")));
                        info_f.setGuiaRemision(rs.getString("GUIAREMISION")==null?"":rs.getString("GUIAREMISION"));
                        info_f.setIdentificacionComprador(rs.getString("IDENTIFICACIONCOMPRADOR"));
                        info_f.setImporteTotal(BigDecimal.valueOf(rs.getDouble("IMPORTETOTAL")));
                        info_f.setMoneda(rs.getString("MONEDA")==null?"":rs.getString("MONEDA"));
                        info_f.setObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD")==null?"":rs.getString("OBLIGADOCONTABILIDAD"));
                        info_f.setPropina(BigDecimal.valueOf(rs.getDouble("PROPINA")));
                        info_f.setRazonSocialComprador(rs.getString("RAZONSOCIALCOMPRADOR").trim());
                        
                        String tipo_id_comp="";
                        if(rs.getInt("TIPOIDENTIFICACIONCOMPRADOR")<10)
                            tipo_id_comp="0"+rs.getString("TIPOIDENTIFICACIONCOMPRADOR");
                        else
                            tipo_id_comp=rs.getString("TIPOIDENTIFICACIONCOMPRADOR");
                        info_f.setTipoIdentificacionComprador(tipo_id_comp);
                        
                        //============================ TOTAL DE IMPUESTO DE LA FACTURA =================================
                        TotalImpuesto total_imp=new TotalImpuesto();
                        total_imp.setBaseImponible(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));
                        total_imp.setCodigo(rs.getInt("CODIGO"));
                        total_imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE"));
                        total_imp.setTarifa(rs.getString("TARIFA"));
                        //OJO CON ESTE CAMPO
                        //REPRESENTA EL VALOR DEL TOTAL DE LOS IMPUESTOS,
                        //EL SRI RETORNA ERROR SI SE ENVIA CON MAS DE 2 DECIMALES

                        total_imp.setValor(BigDecimal.valueOf(rs.getDouble("TOTALIVA")));
                        array_total_imp.getTotalImpuesto().add(total_imp);

                        info_f.setTotalConImpuestos(array_total_imp);
                        
                          
                        info_f.setTotalDescuento(BigDecimal.valueOf(arr.get(i).getTotalDescuento()));
                        info_f.setTotalSinImpuestos(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));


                        //==================================== INFORMACION ADICIONAL =============================
                        InfoAdicional info_a1=new InfoAdicional();
                        info_a1.setNombre("OBSERVACION");
                        String obs=rs.getString("OBSERVACION")==null?"NO REGISTRADO":rs.getString("OBSERVACION").toUpperCase().trim();
                        if(obs!=null)
                        {
                            obs=obs.replaceAll("Á","A");
                            obs=obs.replaceAll("É","E");
                            obs=obs.replaceAll("Í","I");
                            obs=obs.replaceAll("Ó","O");
                            obs=obs.replaceAll("Ú","U");
                            obs=obs.replace("."," ");
                            
//                            obs=obs.replace(",", "");
                            obs=obs.replaceAll("\n", " ");
//                            obs=obs.replace(":", "");
                            obs=obs.replaceAll("ñ", "n");
                            obs=obs.replaceAll("Ñ", "N");
                            obs=obs.replaceAll(",", " ");
                            obs=obs.replaceAll("-", " ");
//                            obs=obs.replace("\",  "");
                            obs=obs.replace("/",  " ");
//                            obs=obs.replace("+", "MAS");
//                            obs=obs.replace("(", " ");
//                            obs=obs.replace(")", " ");
                        }
                        info_a1.setText(obs);
                        
                        
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
                            contacto=contacto.replace("-", " O ");
                            
                        }
                        info_a2.setText(contacto);
                        
                        InfoAdicional info_a3=new InfoAdicional();
                        info_a3.setNombre("DIRECCION");
                        info_a3.setText(rs.getString("DIRECCION")==null?"NO REGISTRADO":rs.getString("DIRECCION").toUpperCase().replace(".", " ").replace("(", " ").replace(")", " ").trim());
                        
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
                            fono=fono.replaceAll("-","");
                        }
                        info_a5.setText(fono);
                        
                        InfoAdicional info_a6=new InfoAdicional();
                        info_a6.setNombre("FONO_ESTAB");
                        info_a6.setText(rs.getString("FONO_ESTAB")==null?"NO REGISTRADO":rs.getString("FONO_ESTAB").replaceAll("-","").trim());
                        
                        
                        array_info_a.getInfoAdicional().add(info_a1);
                        array_info_a.getInfoAdicional().add(info_a2);
                        array_info_a.getInfoAdicional().add(info_a3);
                        array_info_a.getInfoAdicional().add(info_a4);
                        array_info_a.getInfoAdicional().add(info_a5);
                        array_info_a.getInfoAdicional().add(info_a6);

                    }
                    
                      
                    //========================= DATOS DEL DETALLE DE LA FACTURA ============================
                    Detalle detalle=new Detalle();
                    detalle.setCantidad(BigDecimal.valueOf(rs.getDouble("CANTIDAD")));
                    detalle.setCodigoAuxiliar(rs.getString("CODIGOAUXILIAR")==null?"":rs.getString("CODIGOAUXILIAR").replace("-",""));
                    detalle.setCodigoPrincipal(rs.getString("CODIGOPRINCIPAL")==null?"":rs.getString("CODIGOPRINCIPAL"));
                    detalle.setDescripcion(rs.getString("DESCRIPCION")==null?"":rs.getString("DESCRIPCION"));
                    detalle.setDescuento(BigDecimal.valueOf(rs.getDouble("DESCUENTO")));

                        Impuesto imp=new Impuesto();
                        imp.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE_IMP")));
                        imp.setCodigo(rs.getInt("CODIGO_IMP"));
                        imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE_IMP"));
                        imp.setTarifa(rs.getInt("TARIFA_IMP"));
                        imp.setValor(BigDecimal.valueOf(rs.getDouble("VALOR_IMP")));

                        ArrayOfImpuesto array_of_impuesto=new ArrayOfImpuesto();
                        array_of_impuesto.getImpuesto().add(imp);

                    detalle.setImpuestos(array_of_impuesto);
                    detalle.setPrecioTotalSinImpuesto(BigDecimal.valueOf(rs.getDouble("PRECIOTOTALSINIMPUESTO")));
                    detalle.setPrecioUnitario(BigDecimal.valueOf(rs.getDouble("PRECIOUNITARIO")));
                    
                    DetAdicional infoa=new DetAdicional();
                    infoa.setNombre(rs.getString("NOMBRE_ADIC")==null?"NINGUNO":rs.getString("NOMBRE_ADIC"));
                    infoa.setValor(rs.getString("VALOR_ADIC")==null?"NINGUNO":rs.getString("VALOR_ADIC"));
                    
                    ArrayOfDetAdicional arr_ia=new ArrayOfDetAdicional();
                    arr_ia.getDetAdicional().add(infoa);
                    
                    detalle.setDetallesAdicionales(arr_ia);
                    
                    array_det.getDetalle().add(detalle);

                    band++;
                }
              

            }
            catch(SQLException e){e.printStackTrace();}
            finally{
                    rs.close();
                    st.close();
                }
            
                System.out.println("[info] - FACTURA "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                this.MONITOR.setMensajeFacturas("[info] - FACTURA "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                AutorizarFactura autorizar=new AutorizarFactura();
                autorizar.setInfoTributaria( new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                autorizar.setInfoFactura(new JAXBElement(new QName("http://tempuri.org/","infoFactura"),JAXBElement.class,info_f));
                autorizar.setDetalle(new JAXBElement(new QName("http://tempuri.org/","detalle"),JAXBElement.class,array_det));
                autorizar.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                //generando el xml
                generarXML(autorizar,arr.get(i).getEstab(),arr.get(i).getPtoEmi(),arr.get(i).getSecuencial());
                
                
                //Ennviar documento empaquetado al ws SRI para autorizar
                long start=0;
                long stop = 0;
                Response resp=null;
                 
                try{
                    
                resp= new  Response();
//                System.out.println("=============================================");
                System.out.println("[info] - No. Lineas : "+arr.get(i).getLineas());
                this.MONITOR.setMensajeFacturas("[info] - No. Lineas : "+arr.get(i).getLineas());
                System.out.println("[info] - Enviando petición de autorización al WS...");
                this.MONITOR.setMensajeFacturas("[info] - Enviando petición de autorización al WS...");
                
                //obteniendo el tiempo inicial para el tiempo de espera estimado
                start = Calendar.getInstance().getTimeInMillis();      
                
                //Instancia del servicio de INTEME
                //El objeto Response encapsula la información del documento autorizado o no autorizado
                resp=autorizarFactura(info_t,info_f,array_det,array_info_a);
                
                //obteniendo el tiempo final para el tiempo de espera estimado
                stop = Calendar.getInstance().getTimeInMillis();
//                java.util.Date d = new java.util.Date(stop-start);

//                System.out.println("Tiempo de respuesta: "+d.getSeconds()+" seg");
                System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
//                this.MONITOR.setMensajeFacturas("Tiempo de respuesta: "+d.getSeconds()+" seg");
                this.MONITOR.setMensajeFacturas("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                
                enviadas++;
                System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                this.MONITOR.setMensajeFacturas("No. de autorización: "+resp.getAutorizacion().getValue());
                System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                this.MONITOR.setMensajeFacturas("Clave de acceso: "+resp.getClaveAcceso().getValue());
                System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                this.MONITOR.setMensajeFacturas("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                System.out.println("Id. Error: "+resp.getIdError().getValue());
                this.MONITOR.setMensajeFacturas("Id. Error: "+resp.getIdError().getValue());
                System.out.println("Origen: "+resp.getOrigen().getValue());
                this.MONITOR.setMensajeFacturas("Origen: "+resp.getOrigen().getValue());
                System.out.println("Result: "+resp.getResult().getValue());
                this.MONITOR.setMensajeFacturas("Result: "+resp.getResult().getValue());
                System.out.println("Result Data: "+resp.getResultData().getValue());
                this.MONITOR.setMensajeFacturas("Result Data: "+resp.getResultData().getValue());
                    if(resp.getAutorizacion().getValue()!=null)
                    {
                    try{                    
                        //Llamada del metodo para actualizar registro
                        this.MONITOR.setMensajeFacturas("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        int reg=actualizarFactura(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeFacturas("[info] - Registros actualizados : "+reg);
                    }
                    catch(SQLException ex)
                    {
                        this.MONITOR.setMensajeFacturas("[error] - Error al hacer la actualizacion de campos");
                        System.out.println("[error] - Error al hacer la actualizacion de campos");
                    }
                    finally{continue;}
                    }
                    this.MONITOR.setMensajeFacturas("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                    //llamada del metodo para el registro del log
                    
                    notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                    this.MONITOR.setMensajeFacturas("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial");
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeFacturas("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeFacturas("[error] - Ha surgido un error");
                    this.MONITOR.setMensajeFacturas("[error] - "+ex.getCause().getMessage());
                    notificarError(con, ex.getMessage(),info_t,String.valueOf((stop-start)));
                
                }
                finally{
                    if(resp!=null)
                    {resp=null;}
                    continue;
                    
                }
                        
//            }//FINAL DEL IF
        
            }//FINAL DEL FOR
            return enviadas;
    }

    public int enviarFacturas(ConexionBD con,String coddoc){
    
    int enviadas=0;
    InfoTrib infoTrib=null;
    ArrayList<InfoTrib> arr=new ArrayList<>();
    CallableStatement cs=null;
    String sentencia="{call SP_FACTCONSULTACABECERAS(?,?)}";
    String filtro="{call SP_FACTCONSULTADETALLE(?,?,?,?,?)}";
    ResultSet rs=null;
    try{
        cs=con.getCon().prepareCall(sentencia);

        cs.setString(1,coddoc);
        cs.registerOutParameter(2,oracle.jdbc.driver.OracleTypes.CURSOR);

        cs.executeQuery();
        rs=(ResultSet)cs.getObject(2);
        while(rs.next())
        {   
            infoTrib=new InfoTrib();
            infoTrib.setLineas(rs.getString("LINEAS"));
            infoTrib.setEstab(rs.getString("ESTAB"));
            infoTrib.setPtoEmi(rs.getString("PTOEMI"));
            infoTrib.setSecuencial(rs.getString("SECUENCIAL"));
            infoTrib.setTotalSinImpuesto(rs.getDouble("TOTALSINIMPUESTOS"));
            infoTrib.setTotalDescuento(rs.getDouble("TOTALDESCUENTO"));
            arr.add(infoTrib);
        }
        rs.close();
            //Recorriendo el arreglo que contiene documentos listos para enviar
            for(int i=0;i<arr.size();i++)
            {

            this.MONITOR.limpiaFacturas();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arr.size());
            this.MONITOR.setMensajeFacturas("[info] - Registro #"+(i+1)+ " de "+arr.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoFactura info_f=new InfoFactura();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();
            ArrayOfDetalle array_det=new ArrayOfDetalle();
            ArrayOfTotalImpuesto array_total_imp=new ArrayOfTotalImpuesto();
            
            
            //===========================PUEDEN HABER FACTURAS CON VARIOS DETALLES===================================
            int band=0;
            try{
                cs=con.getCon().prepareCall(filtro);
                cs.setString(1, coddoc);
                cs.setString(2, arr.get(i).getEstab());
                cs.setString(3, arr.get(i).getPtoEmi());
                cs.setString(4, arr.get(i).getSecuencial());
                cs.registerOutParameter(5, oracle.jdbc.driver.OracleTypes.CURSOR);


                cs.executeQuery();

                rs=(ResultSet)cs.getObject(5);
               while(rs.next())
                {
                    //validacion para llenar datos de cabecera
                    if(band==0)
                    {

                        //==================================== INFORMACION TRIBUTARIA ===========================
                        info_t.setAmbiente(rs.getInt("AMBIENTE"));
                        info_t.setCodDoc(rs.getString("CODDOC"));
                        info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                        info_t.setEstab(rs.getString("ESTAB"));
                        info_t.setMailCliente(rs.getString("MAILCLIENTE")==null?"":rs.getString("MAILCLIENTE"));
                        info_t.setNombreComercial(rs.getString("NOMBRECOMERCIAL")==null?" ":rs.getString("NOMBRECOMERCIAL"));
                        info_t.setOrigen(rs.getString("ORIGEN")==null?"":rs.getString("ORIGEN"));
                        info_t.setPtoEmi(rs.getString("PTOEMI"));
                        info_t.setRazonSocial(rs.getString("RAZONSOCIAL"));
                         if(rs.getString("RUC").length()<13)
                            info_t.setRuc("0"+rs.getString("RUC"));
                        else
                            info_t.setRuc(rs.getString("RUC"));
                        info_t.setSecuencial(rs.getString("SECUENCIAL"));
                        info_t.setTipoEmision(rs.getInt("TIPOEMISION"));
                        
                        
                        //====================================== INFORMACION DE FACTURA ===========================
                        
                        info_f.setContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                        info_f.setDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO").replace(",", " "));
//                        info_f.setFechaEmision(f.format(rs.getDate("FECHAEMISION")));
                        info_f.setFechaEmision(rs.getString("FECHAEMISION"));
//                        info_f.setGuiaRemision(String.valueOf(rs.getLong("GUIAREMISION")));
                        info_f.setGuiaRemision(rs.getString("GUIAREMISION")==null?"":rs.getString("GUIAREMISION"));
                        info_f.setIdentificacionComprador(rs.getString("IDENTIFICACIONCOMPRADOR"));
                        info_f.setImporteTotal(BigDecimal.valueOf(rs.getDouble("IMPORTETOTAL")));
                        info_f.setMoneda(rs.getString("MONEDA")==null?"":rs.getString("MONEDA"));
                        info_f.setObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD")==null?"":rs.getString("OBLIGADOCONTABILIDAD"));
                        info_f.setPropina(BigDecimal.valueOf(rs.getDouble("PROPINA")));
                        info_f.setRazonSocialComprador(rs.getString("RAZONSOCIALCOMPRADOR").trim());
                        
                        String tipo_id_comp="";
                        if(rs.getInt("TIPOIDENTIFICACIONCOMPRADOR")<10)
                            tipo_id_comp="0"+rs.getString("TIPOIDENTIFICACIONCOMPRADOR");
                        else
                            tipo_id_comp=rs.getString("TIPOIDENTIFICACIONCOMPRADOR");
                        info_f.setTipoIdentificacionComprador(tipo_id_comp);
                        
                        //============================ TOTAL DE IMPUESTO DE LA FACTURA =================================
                        TotalImpuesto total_imp=new TotalImpuesto();
                        total_imp.setBaseImponible(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));
                        total_imp.setCodigo(rs.getInt("CODIGO"));
                        total_imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE"));
                        total_imp.setTarifa(rs.getString("TARIFA"));
                        //OJO CON ESTE CAMPO
                        //REPRESENTA EL VALOR DEL TOTAL DE LOS IMPUESTOS,
                        //EL SRI RETORNA ERROR SI SE ENVIA CON MAS DE 2 DECIMALES

                        total_imp.setValor(BigDecimal.valueOf(rs.getDouble("TOTALIVA")));
                        array_total_imp.getTotalImpuesto().add(total_imp);

                        info_f.setTotalConImpuestos(array_total_imp);
                        
                          
                        info_f.setTotalDescuento(BigDecimal.valueOf(arr.get(i).getTotalDescuento()));
                        info_f.setTotalSinImpuestos(BigDecimal.valueOf(arr.get(i).getTotalSinImpuesto()));


                        //==================================== INFORMACION ADICIONAL =============================
                        InfoAdicional info_a1=new InfoAdicional();
                        info_a1.setNombre("OBSERVACION");
                        String obs=rs.getString("OBSERVACION")==null?"NO REGISTRADO":rs.getString("OBSERVACION").toUpperCase().trim();
                        if(obs!=null)
                        {
                            obs=obs.replaceAll("Á","A");
                            obs=obs.replaceAll("É","E");
                            obs=obs.replaceAll("Í","I");
                            obs=obs.replaceAll("Ó","O");
                            obs=obs.replaceAll("Ú","U");
                            obs=obs.replace("."," ");
                            
//                            obs=obs.replace(",", "");
                            obs=obs.replaceAll("\n", " ");
//                            obs=obs.replace(":", "");
                            obs=obs.replaceAll("ñ", "n");
                            obs=obs.replaceAll("Ñ", "N");
                            obs=obs.replaceAll(",", " ");
                            obs=obs.replaceAll("-", " ");
//                            obs=obs.replace("\",  "");
                            obs=obs.replace("/",  " ");
                        }
                        info_a1.setText(obs);
                        
                        
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
                            contacto=contacto.replace("-", " O ");
                            
                        }
                        info_a2.setText(contacto);
                        
                        InfoAdicional info_a3=new InfoAdicional();
                        info_a3.setNombre("DIRECCION");
                        info_a3.setText(rs.getString("DIRECCION")==null?"NO REGISTRADO":rs.getString("DIRECCION").toUpperCase().replace(".", " ").replace("(", " ").replace(")", " ").trim());
                        
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
                            fono=fono.replaceAll("-","");
                        }
                        info_a5.setText(fono);
                        
                        InfoAdicional info_a6=new InfoAdicional();
                        info_a6.setNombre("FONO_ESTAB");
                        info_a6.setText(rs.getString("FONO_ESTAB")==null?"NO REGISTRADO":rs.getString("FONO_ESTAB").replaceAll("-","").trim());
                        
                        
                        array_info_a.getInfoAdicional().add(info_a1);
                        array_info_a.getInfoAdicional().add(info_a2);
                        array_info_a.getInfoAdicional().add(info_a3);
                        array_info_a.getInfoAdicional().add(info_a4);
                        array_info_a.getInfoAdicional().add(info_a5);
                        array_info_a.getInfoAdicional().add(info_a6);

                    }
                    
                      
                    //========================= DATOS DEL DETALLE DE LA FACTURA ============================
                    Detalle detalle=new Detalle();
                    detalle.setCantidad(BigDecimal.valueOf(rs.getDouble("CANTIDAD")));
                    detalle.setCodigoAuxiliar(rs.getString("CODIGOAUXILIAR")==null?"":rs.getString("CODIGOAUXILIAR").replace("-",""));
                    detalle.setCodigoPrincipal(rs.getString("CODIGOPRINCIPAL")==null?"":rs.getString("CODIGOPRINCIPAL"));
                    detalle.setDescripcion(rs.getString("DESCRIPCION")==null?"":rs.getString("DESCRIPCION"));
                    detalle.setDescuento(BigDecimal.valueOf(rs.getDouble("DESCUENTO")));

                        Impuesto imp=new Impuesto();
                        imp.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE_IMP")));
                        imp.setCodigo(rs.getInt("CODIGO_IMP"));
                        imp.setCodigoPorcentaje(rs.getInt("CODIGOPORCENTAJE_IMP"));
                        imp.setTarifa(rs.getInt("TARIFA_IMP"));
                        imp.setValor(BigDecimal.valueOf(rs.getDouble("VALOR_IMP")));

                        ArrayOfImpuesto array_of_impuesto=new ArrayOfImpuesto();
                        array_of_impuesto.getImpuesto().add(imp);

                    detalle.setImpuestos(array_of_impuesto);
                    detalle.setPrecioTotalSinImpuesto(BigDecimal.valueOf(rs.getDouble("PRECIOTOTALSINIMPUESTO")));
                    detalle.setPrecioUnitario(BigDecimal.valueOf(rs.getDouble("PRECIOUNITARIO")));
                    
                    DetAdicional infoa=new DetAdicional();
                    infoa.setNombre(rs.getString("NOMBRE_ADIC")==null?"NINGUNO":rs.getString("NOMBRE_ADIC"));
                    infoa.setValor(rs.getString("VALOR_ADIC")==null?"NINGUNO":rs.getString("VALOR_ADIC"));
                    
                    ArrayOfDetAdicional arr_ia=new ArrayOfDetAdicional();
                    arr_ia.getDetAdicional().add(infoa);
                    
                    detalle.setDetallesAdicionales(arr_ia);
                    
                    array_det.getDetalle().add(detalle);

                    band++;
                }
            }
            catch(SQLException e){System.out.println("[error] - Ha surgido un error al realizar el recorrido del detalle");}
            finally{rs.close();}
            
                System.out.println("[info] - FACTURA "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                this.MONITOR.setMensajeFacturas("[info] - FACTURA "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                AutorizarFactura autorizar=new AutorizarFactura();
                autorizar.setInfoTributaria( new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                autorizar.setInfoFactura(new JAXBElement(new QName("http://tempuri.org/","infoFactura"),JAXBElement.class,info_f));
                autorizar.setDetalle(new JAXBElement(new QName("http://tempuri.org/","detalle"),JAXBElement.class,array_det));
                autorizar.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                //generando el xml
                generarXML(autorizar,arr.get(i).getEstab(),arr.get(i).getPtoEmi(),arr.get(i).getSecuencial());
                
                
                //Ennviar documento empaquetado al ws SRI para autorizar
                long start=0;
                long stop = 0;
                Response resp=null;
                 
                try{
                    
                resp= new  Response();
//                System.out.println("=============================================");
                System.out.println("[info] - No. Lineas : "+arr.get(i).getLineas());
                this.MONITOR.setMensajeFacturas("[info] - No. Lineas : "+arr.get(i).getLineas());
                System.out.println("[info] - Enviando petición de autorización al WS...");
                this.MONITOR.setMensajeFacturas("[info] - Enviando petición de autorización al WS...");
                
                //obteniendo el tiempo inicial para el tiempo de espera estimado
                start = Calendar.getInstance().getTimeInMillis();      
                
                //Instancia del servicio de INTEME
                //El objeto Response encapsula la información del documento autorizado o no autorizado
                resp=autorizarFactura(info_t,info_f,array_det,array_info_a);
                
                //obteniendo el tiempo final para el tiempo de espera estimado
                stop = Calendar.getInstance().getTimeInMillis();
//                java.util.Date d = new java.util.Date(stop-start);

//                System.out.println("Tiempo de respuesta: "+d.getSeconds()+" seg");
                System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
//                this.MONITOR.setMensajeFacturas("Tiempo de respuesta: "+d.getSeconds()+" seg");
                this.MONITOR.setMensajeFacturas("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                
                enviadas++;
                System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                this.MONITOR.setMensajeFacturas("No. de autorización: "+resp.getAutorizacion().getValue());
                System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                this.MONITOR.setMensajeFacturas("Clave de acceso: "+resp.getClaveAcceso().getValue());
                System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                this.MONITOR.setMensajeFacturas("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                System.out.println("Id. Error: "+resp.getIdError().getValue());
                this.MONITOR.setMensajeFacturas("Id. Error: "+resp.getIdError().getValue());
                System.out.println("Origen: "+resp.getOrigen().getValue());
                this.MONITOR.setMensajeFacturas("Origen: "+resp.getOrigen().getValue());
                System.out.println("Result: "+resp.getResult().getValue());
                this.MONITOR.setMensajeFacturas("Result: "+resp.getResult().getValue());
                System.out.println("Result Data: "+resp.getResultData().getValue());
                this.MONITOR.setMensajeFacturas("Result Data: "+resp.getResultData().getValue());
                    if(resp.getAutorizacion().getValue()!=null)
                    {
                    try{                    
                        //Llamada del metodo para actualizar registro
                        this.MONITOR.setMensajeFacturas("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        int reg=actualizarFactura(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeFacturas("[info] - Registros actualizados : "+reg);
                    }
                    catch(SQLException ex)
                    {
                        this.MONITOR.setMensajeFacturas("[error] - Error al hacer la actualizacion de campos");
                        System.out.println("[error] - Error al hacer la actualizacion de campos");
                    }
                    finally{continue;}
                    }
                    this.MONITOR.setMensajeFacturas("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                    //llamada del metodo para el registro del log
                    
                    notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                    this.MONITOR.setMensajeFacturas("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial");
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeFacturas("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeFacturas("[error] - Ha surgido un error");
                    this.MONITOR.setMensajeFacturas("[error] - "+ex.getCause().getMessage());
                    notificarError(con, ex.getMessage(),info_t,String.valueOf((stop-start)));
                
                }
                finally{
                    if(resp!=null)
                    {resp=null;}
                    continue;
                    
                }
        
            }//FINAL DEL FOR

    }//final del try
    catch(SQLException ex){System.out.println("[error] - Error al realizar la consulta de la cabecera");}
    finally{
             try {
             rs.close();
             cs.close();
             
             } catch (SQLException ex) {
                 System.out.println("[error] - Error al cerrar recursos (ResultSet,CallableStatement)");
             }
            }
    
    return enviadas;
        
    }
    
    public int actualizarFactura(ConexionBD con,Response autorizacion,InfoTributaria info )throws SQLException{
       
        String update="UPDATE INVE_DOCUMENTOS_FE_DAT SET NUME_AUTO_INVE_DOCU=? "
                + "WHERE CODDOC='01' AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                + "AND ESTAB="+info.getEstab()+" AND PTOEMI="+info.getPtoEmi()+" AND SECUENCIAL="+info.getSecuencial() ;
        PreparedStatement ps = con.getCon().prepareStatement(update);
        ps.setString(1, autorizacion.getAutorizacion().getValue());
        int result=ps.executeUpdate();
        ps.close();
        
    return result;
    
    }
    
    public void generarXML(AutorizarFactura autorizar,String estab, String ptoEmi,String secuencial){
    
        Marshaller m=null;
        String rutaXml=null;
        JAXBElement<AutorizarFactura> jaxb_autoriza=null;
        JAXBContext jaxbContext=null;
        try{
            System.out.println("[info] - Generando xml...");  
            this.MONITOR.setMensajeFacturas("[info] - Generando xml...");
 
            jaxb_autoriza=new JAXBElement(new QName(AutorizarFactura.class.getSimpleName()),AutorizarFactura.class,autorizar);
            jaxbContext=JAXBContext.newInstance(AutorizarFactura.class);
            m=jaxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
            rutaXml=this.MONITOR.dir_facturas+"AutorizarFactura"+estab+"-"+ptoEmi+"-"+secuencial+".xml";
            m.marshal(jaxb_autoriza, new File (rutaXml)); 

            System.out.println("[info] - xml generado "+rutaXml);  
            this.MONITOR.setMensajeFacturas("[info] - xml generado "+rutaXml);
    
    }
    catch(JAXBException ex){
        System.out.println("[error] - Error al generar xml");  
            this.MONITOR.setMensajeFacturas("[error] - Error al generar xml");}
    finally{}
    
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
    
        String update="UPDATE INVE_INFO_FE_DAT SET ESTATUS=?,USUARIO_ACT=?,ULT_EJECUCION=?,HOST_ACT=?,ATENDIENDO=? WHERE NOMBRE='HILO FACTURAS'";
        
        PreparedStatement ps = con.getCon().prepareStatement(update);
        ps.setString(1, estado);
        ps.setString(2, System.getProperty("user.name"));
        
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
//        String cad=now.getYear()+"/"+now.getMonth()+"/"+now.getDay();
//        SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy");
//        System.out.println(f.format(now));
        
        ps.setDate(3,new Date(now.getYear(), now.getMonth(), now.getDay()));
        InetAddress localHost = InetAddress.getLocalHost();
        ps.setString(4,localHost.getHostName());
        ps.setInt(5, atendiendo);
        
        int result=ps.executeUpdate();
        ps.close();
    return result;
    }

    private Response autorizarFactura(fedaemonfinal.InfoTributaria infoTributaria, fedaemonfinal.InfoFactura infoFactura, fedaemonfinal.ArrayOfDetalle detalle, fedaemonfinal.ArrayOfInfoAdicional infoAdicional) {
        Response respuesta = null;
        fedaemonfinal.CloudAutorizarComprobante service = new fedaemonfinal.CloudAutorizarComprobante();
        fedaemonfinal.IcloudAutorizarComprobante port = service.getBasicHttpBindingIcloudAutorizarComprobante();
        try 
        {            
            respuesta = new Response();            
            respuesta = port.autorizarFactura(infoTributaria, infoFactura, detalle, infoAdicional);            
        } 
        catch (Exception e) 
        {
            System.err.println("Error al invocar: " + e.getMessage());
            this.MONITOR.setMensajeFacturas("Error al invocar: " + e.getMessage());
        }
        finally
        {
            service = null;
            port = null;      
        }

//             System.err.println("Puerto: " + service.getPorts().toString());
        return respuesta;
        
    }
    
    public frmMonitor getMONITOR() {
        return MONITOR;
    }

    public void setMONITOR(frmMonitor MONITOR) {
        this.MONITOR = MONITOR;
    }
    
}
