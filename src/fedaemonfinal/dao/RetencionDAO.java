

package fedaemonfinal.dao;

import fedaemonfinal.ArrayOfImpuestosRetencion;
import fedaemonfinal.ArrayOfInfoAdicional;
import fedaemonfinal.AutorizarComprobanteRetencion;
import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.ImpuestosRetencion;
import fedaemonfinal.InfoAdicional;
import fedaemonfinal.InfoCompRetencion;
import fedaemonfinal.util.InfoTrib;
import fedaemonfinal.InfoTributaria;
import fedaemonfinal.Response;
import fedaemonfinal.frms.frmMonitor;
import java.io.File;
import java.math.BigDecimal;
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
 * @author Michael Beltrán
 */
public final class RetencionDAO {
    
    protected frmMonitor MONITOR;
    
    public int consultarRetencionPendientes(ConexionBD con)throws Exception{
    int result=0;
    String select="SELECT COUNT(*) FROM INVE_RETENCIONES_FE_DAT "
            + "WHERE NUME_AUTO_INVE_RETE IS NULL AND CODDOC='07' AND AMBIENTE=2 "
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
    
    public int consultarRetencionPendientes(ConexionBD con,String coddoc) {
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
    
    public int enviarRetenciones(ConexionBD con)throws Exception{
        int enviadas=0;
        //OJO que al consultar data de la base se recuperará info como estaba hasta el ultimo COMMIT ejecutado
        String select="SELECT COUNT(*),ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "FROM INVE_RETENCIONES_FE_DAT "
                + "WHERE CODDOC='07' "
                + "AND NUME_AUTO_INVE_RETE IS NULL AND AMBIENTE=2 "
//                + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                + "GROUP BY ESTAB,PTOEMI,SECUENCIAL,FECHAEMISION "
                + "ORDER BY FECHAEMISION ASC,SECUENCIAL ASC";
        Statement st= con.getCon().createStatement();
        ResultSet rs=st.executeQuery(select);
        InfoTrib fra=null;
        ArrayList<InfoTrib> arr=new ArrayList<>();
//        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        while(rs.next())
        {   
            fra=new InfoTrib();
            fra.setEstab(rs.getString("ESTAB"));
            fra.setPtoEmi(rs.getString("PTOEMI"));
            fra.setSecuencial(rs.getString("SECUENCIAL"));
            arr.add(fra);
        }
        rs.close();
        st.close();
        
        for(int i=0;i<arr.size();i++)
        {
            this.MONITOR.limpiaRetenciones();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arr.size());
            this.MONITOR.setMensajeRetenciones("[info] - Registro #"+(i+1)+ " de "+arr.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoCompRetencion info_comp=new InfoCompRetencion();
            ArrayOfImpuestosRetencion array_impuestos=new ArrayOfImpuestosRetencion();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();
            
            int band=0;
            try{
                    st=con.getCon().createStatement();
                    String filtro="SELECT * FROM INVE_RETENCIONES_FE_DAT WHERE NUME_AUTO_INVE_RETE IS NULL AND CODDOC='07' AND AMBIENTE=2 "
//                            + "AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101'"
                                +" AND ESTAB="+arr.get(i).getEstab()
                                +" AND PTOEMI="+arr.get(i).getPtoEmi()
                                +" AND SECUENCIAL="+arr.get(i).getSecuencial();
                    rs=st.executeQuery(filtro);
                    
                    while(rs.next())
                    {
                        
                        if(band==0)
                        {
                            //======================  INFORMACION TRIBUTARIA ===========================================
                            info_t.setAmbiente(rs.getInt("AMBIENTE"));
                            info_t.setCodDoc(rs.getString("CODDOC"));
                            info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                            info_t.setEstab(rs.getString("ESTAB"));
                            info_t.setMailCliente(rs.getString("MAILCLIENTE"));
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
                            


                            //======================= INFORMACION DEL COMPROBANTE ==========================================

                            info_comp.setContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                            info_comp.setDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO"));
                            info_comp.setFechaEmision(rs.getString("FECHAEMISION"));
                            info_comp.setIdentificacionSujetoRetenido(rs.getString("IDENTIFICACIONSUJETORETENIDO"));
                            info_comp.setObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD"));
                            info_comp.setPeriodoFiscal(rs.getString("PERIODOFISCAL"));
                            info_comp.setRazonSocialSujetoRetenido(rs.getString("RAZONSOCIALSUJETORETENIDO"));
                            info_comp.setTipoIdentificacionSujetoRetenido(rs.getString("TIPOIDENTIFICACIONSUJETORETE"));
                            
                            
                            //======================= INFORMACION ADICIONAL==========================================
                            InfoAdicional info_a1=new InfoAdicional();
                            info_a1.setNombre("OBSERVACION");
                            String obs=rs.getString("OBSERVACION")==null?"NO REGISTRADO":rs.getString("OBSERVACION").toUpperCase().trim();
                            if(obs!=null)
                            {
                                obs=obs.replace('Á','A');
                                obs=obs.replace('É','E');
                                obs=obs.replace('Í','I');
                                obs=obs.replace('Ó','O');
                                obs=obs.replace('Ú','U');
    //                            obs=obs.replace(".", "");
    //                            obs=obs.replace(",", "");
                                obs=obs.replace("\n", "");
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

                            //========================== INFORMACION DE IMPUESTOS =======================================
                             ImpuestosRetencion impuestos=new ImpuestosRetencion();
                             impuestos.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE")));
                             impuestos.setCodDocSustento(rs.getString("CODDOCSUSTENTO"));
                             impuestos.setCodigo(Integer.parseInt(rs.getString("CODIGO")));
                             impuestos.setCodigoRetencion(rs.getString("CODIGORETENCION"));
                             impuestos.setFechaEmisionDocSustento(rs.getString("FECHAEMISIONDOCSUSTENTO"));
                             
//                              if(rs.getString("NUMDOCSUSTENTO").length()==15)
                                impuestos.setNumDocSustento(rs.getString("NUMDOCSUSTENTO"));
//                             else
//                                impuestos.setNumDocSustento(rs.getString("NUMDOCSUSTENTO"));
                             impuestos.setPorcentajeRetener(rs.getInt("PORCENTAJERETENER"));
                             impuestos.setValorRetenido(BigDecimal.valueOf(rs.getDouble("VALORRETENIDO")));
                            


                            array_impuestos.getImpuestosRetencion().add(impuestos);

                        band++;
                    }
                }
                catch(SQLException e){e.printStackTrace();}
                finally{
                    rs.close();
                    st.close();
                }
         
                 //=============================Formando el xml... ====================================
                System.out.println("[info] - COMPROBANTE RETENCION "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                this.MONITOR.setMensajeRetenciones("[info] - COMPROBANTE RETENCION "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                AutorizarComprobanteRetencion autorizar=new AutorizarComprobanteRetencion();
                autorizar.setInfoTributaria(new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                autorizar.setInfoCompRetencion(new JAXBElement(new QName("http://tempuri.org/","infoCompRetencion"),JAXBElement.class,info_comp));
                autorizar.setImpuestos(new JAXBElement(new QName("http://tempuri.org/","impuestos"),JAXBElement.class,array_impuestos));
                autorizar.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                generarXML(autorizar,arr.get(i).getEstab(),arr.get(i).getPtoEmi(),arr.get(i).getSecuencial());
            
                long start=0;
                long stop = 0;
                Response resp=null;
                 
                try{
                    resp=new Response();
                System.out.println("[info] - Enviando petición de autorización al WS...");
                this.MONITOR.setMensajeRetenciones("[info] - Enviando petición de autorización al WS...");
                //obteniendo el tiempo inicial para el tiempo de espera estimado
                start = Calendar.getInstance().getTimeInMillis();
                //Instancia del servicio de INTEME
                //El objeto Response encapsula la información del documento autorizado o no autorizado
                resp=autorizarComprobanteRetencion(info_t,info_comp,array_impuestos,array_info_a); 
                 //obteniendo el tiempo final para el tiempo de espera estimado
                stop = Calendar.getInstance().getTimeInMillis();
//                java.util.Date d = new java.util.Date(stop-start);

//                System.out.println("Tiempo de respuesta: "+d.getSeconds()+" seg");
                System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
//                this.MONITOR.setMensajeRetenciones("Tiempo de respuesta: "+d.getSeconds()+" seg");
                this.MONITOR.setMensajeRetenciones("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                
                enviadas++;

                System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                this.MONITOR.setMensajeRetenciones("No. de autorización: "+resp.getAutorizacion().getValue());
                System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                this.MONITOR.setMensajeRetenciones("Clave de acceso: "+resp.getClaveAcceso().getValue());
                System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                this.MONITOR.setMensajeRetenciones("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                System.out.println("Id. Error: "+resp.getIdError().getValue());
                this.MONITOR.setMensajeRetenciones("Id. Error: "+resp.getIdError().getValue());
                System.out.println("Origen: "+resp.getOrigen().getValue());
                this.MONITOR.setMensajeRetenciones("Origen: "+resp.getOrigen().getValue());
                System.out.println("Result: "+resp.getResult().getValue());
                this.MONITOR.setMensajeRetenciones("Result: "+resp.getResult().getValue());
                System.out.println("Result Data: "+resp.getResultData().getValue());
                this.MONITOR.setMensajeRetenciones("Result Data: "+resp.getResultData().getValue());

                if(resp.getAutorizacion().getValue()!=null)
                {
                    try{
                        this.MONITOR.setMensajeRetenciones("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        //llamada del metodo para actualizar registro
                        int reg=actualizarRetencion(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeRetenciones("[info] - Registros actualizados : "+reg);
                     }
                    catch(SQLException ex)
                    {
                    this.MONITOR.setMensajeRetenciones("[error] - Error al hacer la actualizacion de campos");
                    System.out.println("[error] - Error al hacer la actualizacion de campos");
                    }
                }
                this.MONITOR.setMensajeRetenciones("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                //llamada del metodo para el registro del log
                notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                this.MONITOR.setMensajeRetenciones("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial"); 
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeRetenciones("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeRetenciones("[error] - Ha surgido un error\n"+ex.getMessage());
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
    
    public int enviarRetenciones(ConexionBD con,String coddoc){
        int enviadas=0;
        CallableStatement cs=null;
        String sentencia="{call SP_FACTCONSULTACABECERAS(?,?)}";
        String filtro="{call SP_FACTCONSULTADETALLE(?,?,?,?,?)}";
        ResultSet rs=null;
        InfoTrib infoTrib=null;
        ArrayList<InfoTrib> arr=new ArrayList<>();
        
         try{
            //me devuelve informacion preliminar de los pendientes
            cs=con.getCon().prepareCall(sentencia);
    
            cs.setString(1, coddoc);
            cs.registerOutParameter(2, oracle.jdbc.driver.OracleTypes.CURSOR);

            cs.executeQuery();
    
            rs=(ResultSet)cs.getObject(2);
            //almacenamos en una lista
            while(rs.next())
            {   
                infoTrib=new InfoTrib();
                infoTrib.setEstab(rs.getString("ESTAB"));
                infoTrib.setPtoEmi(rs.getString("PTOEMI"));
                infoTrib.setSecuencial(rs.getString("SECUENCIAL"));
                
                arr.add(infoTrib);
            }
            rs.close();
            //recorremos uno a uno los elementos de la lista
            for(int i=0;i<arr.size();i++)
            {
            this.MONITOR.limpiaRetenciones();
            System.out.println("[info] - Registro #"+(i+1)+ " de "+arr.size());
            this.MONITOR.setMensajeRetenciones("[info] - Registro #"+(i+1)+ " de "+arr.size());
            InfoTributaria info_t=new InfoTributaria();
            InfoCompRetencion info_comp=new InfoCompRetencion();
            ArrayOfImpuestosRetencion array_impuestos=new ArrayOfImpuestosRetencion();
            ArrayOfInfoAdicional array_info_a=new ArrayOfInfoAdicional();
            
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
                        //validacion para llenar cabecera
                        if(band==0)
                        {
                            //======================  INFORMACION TRIBUTARIA ===========================================
                            info_t.setAmbiente(rs.getInt("AMBIENTE"));
                            info_t.setCodDoc(rs.getString("CODDOC"));
                            info_t.setDirMatriz(rs.getString("DIRMATRIZ"));
                            info_t.setEstab(rs.getString("ESTAB"));
                            info_t.setMailCliente(rs.getString("MAILCLIENTE"));
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
                            


                            //======================= INFORMACION DEL COMPROBANTE ==========================================

                            info_comp.setContribuyenteEspecial(rs.getString("CONTRIBUYENTEESPECIAL"));
                            info_comp.setDirEstablecimiento(rs.getString("DIRESTABLECIMIENTO"));
                            info_comp.setFechaEmision(rs.getString("FECHAEMISION"));
                            info_comp.setIdentificacionSujetoRetenido(rs.getString("IDENTIFICACIONSUJETORETENIDO"));
                            info_comp.setObligadoContabilidad(rs.getString("OBLIGADOCONTABILIDAD"));
                            info_comp.setPeriodoFiscal(rs.getString("PERIODOFISCAL"));
                            info_comp.setRazonSocialSujetoRetenido(rs.getString("RAZONSOCIALSUJETORETENIDO"));
                            info_comp.setTipoIdentificacionSujetoRetenido(rs.getString("TIPOIDENTIFICACIONSUJETORETE"));
                            
                            
                            //======================= INFORMACION ADICIONAL==========================================
                            InfoAdicional info_a1=new InfoAdicional();
                            info_a1.setNombre("OBSERVACION");
                            String obs=rs.getString("OBSERVACION")==null?"NO REGISTRADO":rs.getString("OBSERVACION").toUpperCase().trim();
                            if(obs!=null)
                            {
                                obs=obs.replace('Á','A');
                                obs=obs.replace('É','E');
                                obs=obs.replace('Í','I');
                                obs=obs.replace('Ó','O');
                                obs=obs.replace('Ú','U');
    //                            obs=obs.replace(".", "");
    //                            obs=obs.replace(",", "");
                                obs=obs.replace("\n", "");
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
                        
                        }//de aqui para abajo todo es parte del detalle

                            //========================== INFORMACION DE IMPUESTOS =======================================
                             ImpuestosRetencion impuestos=new ImpuestosRetencion();
                             impuestos.setBaseImponible(BigDecimal.valueOf(rs.getDouble("BASEIMPONIBLE")));
                             impuestos.setCodDocSustento(rs.getString("CODDOCSUSTENTO"));
                             impuestos.setCodigo(Integer.parseInt(rs.getString("CODIGO")));
                             impuestos.setCodigoRetencion(rs.getString("CODIGORETENCION"));
                             impuestos.setFechaEmisionDocSustento(rs.getString("FECHAEMISIONDOCSUSTENTO"));
                             
//                              if(rs.getString("NUMDOCSUSTENTO").length()==15)
                                impuestos.setNumDocSustento(rs.getString("NUMDOCSUSTENTO"));
//                             else
//                                impuestos.setNumDocSustento(rs.getString("NUMDOCSUSTENTO"));
                             impuestos.setPorcentajeRetener(rs.getInt("PORCENTAJERETENER"));
                             impuestos.setValorRetenido(BigDecimal.valueOf(rs.getDouble("VALORRETENIDO")));
                            


                            array_impuestos.getImpuestosRetencion().add(impuestos);

                        band++;
                    }//final del while del ResultSet
                }
                catch(SQLException e){e.printStackTrace();}
                finally{
                    rs.close();
                   
                }
         
                 //=============================Formando el xml... ====================================
                System.out.println("[info] - COMPROBANTE RETENCION "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                this.MONITOR.setMensajeRetenciones("[info] - COMPROBANTE RETENCION "+arr.get(i).getEstab()+"-"+arr.get(i).getPtoEmi()+"-"+arr.get(i).getSecuencial());
                AutorizarComprobanteRetencion autorizar=new AutorizarComprobanteRetencion();
                autorizar.setInfoTributaria(new JAXBElement(new QName("http://tempuri.org/","infoTributaria"),JAXBElement.class,info_t));
                autorizar.setInfoCompRetencion(new JAXBElement(new QName("http://tempuri.org/","infoCompRetencion"),JAXBElement.class,info_comp));
                autorizar.setImpuestos(new JAXBElement(new QName("http://tempuri.org/","impuestos"),JAXBElement.class,array_impuestos));
                autorizar.setInfoAdicional(new JAXBElement(new QName("http://tempuri.org/","infoAdicional"),JAXBElement.class,array_info_a));

                generarXML(autorizar,arr.get(i).getEstab(),arr.get(i).getPtoEmi(),arr.get(i).getSecuencial());
            
                long start=0;
                long stop = 0;
                Response resp=null;
                 
                try{
                    resp=new Response();
                System.out.println("[info] - Enviando petición de autorización al WS...");
                this.MONITOR.setMensajeRetenciones("[info] - Enviando petición de autorización al WS...");
                //obteniendo el tiempo inicial para el tiempo de espera estimado
                start = Calendar.getInstance().getTimeInMillis();
                //Instancia del servicio de INTEME
                //El objeto Response encapsula la información del documento autorizado o no autorizado
                resp=autorizarComprobanteRetencion(info_t,info_comp,array_impuestos,array_info_a); 
                 //obteniendo el tiempo final para el tiempo de espera estimado
                stop = Calendar.getInstance().getTimeInMillis();
//                java.util.Date d = new java.util.Date(stop-start);

//                System.out.println("Tiempo de respuesta: "+d.getSeconds()+" seg");
                System.out.println("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
//                this.MONITOR.setMensajeRetenciones("Tiempo de respuesta: "+d.getSeconds()+" seg");
                this.MONITOR.setMensajeRetenciones("[info] - Tiempo de respuesta: "+(stop-start)+" miliseg");
                
                enviadas++;

                System.out.println("No. de autorización: "+resp.getAutorizacion().getValue());
                this.MONITOR.setMensajeRetenciones("No. de autorización: "+resp.getAutorizacion().getValue());
                System.out.println("Clave de acceso: "+resp.getClaveAcceso().getValue());
                this.MONITOR.setMensajeRetenciones("Clave de acceso: "+resp.getClaveAcceso().getValue());
                System.out.println("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                this.MONITOR.setMensajeRetenciones("Fecha Autorización: "+resp.getFechaAutorizacion().getValue());
                System.out.println("Id. Error: "+resp.getIdError().getValue());
                this.MONITOR.setMensajeRetenciones("Id. Error: "+resp.getIdError().getValue());
                System.out.println("Origen: "+resp.getOrigen().getValue());
                this.MONITOR.setMensajeRetenciones("Origen: "+resp.getOrigen().getValue());
                System.out.println("Result: "+resp.getResult().getValue());
                this.MONITOR.setMensajeRetenciones("Result: "+resp.getResult().getValue());
                System.out.println("Result Data: "+resp.getResultData().getValue());
                this.MONITOR.setMensajeRetenciones("Result Data: "+resp.getResultData().getValue());

                if(resp.getAutorizacion().getValue()!=null)
                {
                    try{
                        this.MONITOR.setMensajeRetenciones("[info] - Actualizando registros...");
                        System.out.println("[info] - Actualizando registros...");
                        //llamada del metodo para actualizar registro
                        int reg=actualizarRetencion(con, resp,info_t);
                        System.out.println("[info] - Registros actualizados : "+reg);
                        this.MONITOR.setMensajeRetenciones("[info] - Registros actualizados : "+reg);
                     }
                    catch(SQLException ex)
                    {
                    this.MONITOR.setMensajeRetenciones("[error] - Error al hacer la actualizacion de campos");
                    System.out.println("[error] - Error al hacer la actualizacion de campos");
                    }
                }
                this.MONITOR.setMensajeRetenciones("[info] - Registrando en el log...");
                    System.out.println("[info] - Registrando en el log...");
                //llamada del metodo para el registro del log
                notificarResultado(con, resp,info_t,String.valueOf((stop-start)));
                this.MONITOR.setMensajeRetenciones("[info] - Evento capturado en el historial");
                    System.out.println("[info] - Evento capturado en el historial"); 
                }catch(SQLException ex){
                    stop = Calendar.getInstance().getTimeInMillis();
                    System.out.println("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    this.MONITOR.setMensajeRetenciones("[info] - Tiempo de espera: "+(stop-start)+" miliseg");
                    //llamada del metodo para el registro del log
                    this.MONITOR.setMensajeRetenciones("[error] - Ha surgido un error\n"+ex.getMessage());
                    notificarError(con, ex.getMessage(),info_t,String.valueOf((stop-start)));
                
                }
                finally{
                    if(resp!=null)
                    {resp=null;}
                    continue;
                }

            }//final del for
         }
         catch(SQLException sqle){sqle.printStackTrace();}
         finally{
             try {
             rs.close();
             cs.close();
             
             } catch (SQLException ex) {
                 ex.printStackTrace();
             }
            }
    
    return enviadas;
       
    }
    
    public void generarXML(AutorizarComprobanteRetencion autorizar,String estab,String ptoEmi,String secuencial){
    
    System.out.println("[info] - Generando xml...");  
                this.MONITOR.setMensajeRetenciones("[info] - Generando xml...");
                Marshaller m;
                String rutaXml=null;
                JAXBElement<AutorizarComprobanteRetencion> jaxb_autoriza=null;
                JAXBContext jaxb_context=null;
                try{
                jaxb_autoriza=new JAXBElement(new QName(AutorizarComprobanteRetencion.class.getSimpleName()),AutorizarComprobanteRetencion.class,autorizar);
                jaxb_context=JAXBContext.newInstance(AutorizarComprobanteRetencion.class);
                m=jaxb_context.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT,true);
                rutaXml=this.MONITOR.dir_retencion+"AutorizarComprobanteRetencion"+estab+"-"+ptoEmi+"-"+secuencial+".xml";
                m.marshal(jaxb_autoriza, new File (rutaXml));
                
                System.out.println("[info] - xml generado "+rutaXml);  
                this.MONITOR.setMensajeRetenciones("[info] - xml generado "+rutaXml);
                }
                catch(JAXBException ex){}
                finally{}
    }
    
    public int actualizarRetencion(ConexionBD con,Response autorizacion,InfoTributaria info)throws SQLException{
        
        String update="UPDATE INVE_RETENCIONES_FE_DAT SET NUME_AUTO_INVE_RETE=? "
                + "WHERE CODDOC='07' AND AMBIENTE=2 "
//                + " AND CODI_ADMI_EMPR_FINA='00001' AND CODI_ADMI_PUNT_VENT='101' "
                + " AND ESTAB="+info.getEstab()+" AND PTOEMI="+info.getPtoEmi()+" AND SECUENCIAL="+info.getSecuencial() ;
        PreparedStatement ps=con.getCon().prepareStatement(update);
        ps.setString(1, autorizacion.getAutorizacion().getValue());
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
        ps.setInt(5,2);

        int n=ps.executeUpdate();
        ps.close();
        return n;

    }
    
    public int cambiaEstado(ConexionBD con,String estado,int atendiendo)throws SQLException, UnknownHostException{
    
        String update="UPDATE INVE_INFO_FE_DAT SET ESTATUS=?,USUARIO_ACT=?,ULT_EJECUCION=?,HOST_ACT=?,ATENDIENDO=? WHERE NOMBRE='HILO RETENCIONES'";
        
        PreparedStatement ps = con.getCon().prepareStatement(update);
        ps.setString(1, estado);
        ps.setString(2, System.getProperty("user.name"));
        
        Calendar calendar = Calendar.getInstance();
        java.util.Date now = calendar.getTime();
//        String cad=now.getYear()+"/"+now.getMonth()+"/"+now.getDay();
        SimpleDateFormat f=new SimpleDateFormat("dd/MM/yyyy");
//        System.out.println(f.format(now));
        
        ps.setDate(3,new Date(now.getYear(), now.getMonth(), now.getDay()));
        InetAddress localHost = InetAddress.getLocalHost();
        ps.setString(4,localHost.getHostName());
        ps.setInt(5, atendiendo);
        
        int result=ps.executeUpdate();
        ps.close();
    return result;
    }

    private static Response autorizarComprobanteRetencion(fedaemonfinal.InfoTributaria infoTributaria, fedaemonfinal.InfoCompRetencion infoCompRetencion, fedaemonfinal.ArrayOfImpuestosRetencion impuestos, fedaemonfinal.ArrayOfInfoAdicional infoAdicional) {
        Response respuesta = null;
        fedaemonfinal.CloudAutorizarComprobante service = new fedaemonfinal.CloudAutorizarComprobante();
        fedaemonfinal.IcloudAutorizarComprobante port = service.getBasicHttpBindingIcloudAutorizarComprobante();
        try 
        {            
            respuesta = new Response();
            respuesta=port.autorizarComprobanteRetencion(infoTributaria, infoCompRetencion, impuestos, infoAdicional);
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
