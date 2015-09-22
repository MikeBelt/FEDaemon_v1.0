
package fedaemonfinal.hilos;

import fedaemonfinal.util.ConexionBD;
import fedaemonfinal.dao.RetencionDAO;
import fedaemonfinal.frms.frmMonitor;
import java.sql.SQLException;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarComprobantesRetencion extends Thread{
    
    protected ConexionBD CONEXION;
    protected frmMonitor MONITOR;
    public RetencionDAO ret;
    
    @Override
    public void run(){
//    ConexionBD CONEXION=new ConexionBD("inter","INTER2014","192.168.1.245","GLTEVCOL");
//    ConexionBD CONEXION=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
       int enviadas=0;
       int contar=0;
       ConexionBD con=new ConexionBD(CONEXION.getUsr(),CONEXION.getPass(),CONEXION.getServer(),CONEXION.getBase());
       ret=new RetencionDAO();
       ret.setMonitor(MONITOR);
    
        System.out.println("[info] - Iniciando hilo para autorización de Retenciones... ");
        this.MONITOR.setMensajeRetenciones("[info] - Iniciando hilo para autorización de Retenciones... ");            
        while(true)
        {
            this.MONITOR.cambiaEstadoPanel("jPRetencion", "Retenciones [EJECUTANDO]");
            this.MONITOR.limpiaRetenciones();
            try
            {
                System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
                this.MONITOR.setMensajeRetenciones("[info] - Estableciendo conexión con la Base de Datos... ");
                con.conectar();

                System.out.println("[info] - Conexión para hilo retenciones exitosa");
                this.MONITOR.setMensajeRetenciones("[info] - Conexión para hilo retenciones exitosa");

                System.out.println("[info] - Verificando Comprobantes de Retención pendientes de autorización...");
                this.MONITOR.setMensajeRetenciones("[info] - Verificando Comprobantes de Retención pendientes de autorización...");
        
                contar=ret.consultarRetencionPendientes(con,"07");
                ret.cambiaEstado(con, "EJECUTANDO", contar);
                
                if(contar==0)
                { 
                    System.out.println("[info] - No hay retenciones pendientes de autorización.");
                    this.MONITOR.setMensajeRetenciones("[info] - No hay retenciones pendientes de autorización.");

                }
                else
                {
                    System.out.println("[info] - Petición de autorización para: "+contar+" retenciones");
                    this.MONITOR.setMensajeRetenciones("[info] - Petición de autorización para: "+contar+" retenciones");
                    enviadas=ret.enviarRetenciones(con);
                    System.out.println("[info] - Se han enviado: "+enviadas+" comprobantes de retención para autorización del SRI.");
                    this.MONITOR.setMensajeRetenciones("[info] - Se han enviado: "+enviadas+" comprobantes de retención para autorización del SRI.");

                }
            }
            catch(SQLException sqlex)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
                this.MONITOR.setMensajeRetenciones("[Error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
            }
            catch(ClassNotFoundException cnfe)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
                this.MONITOR.setMensajeRetenciones("[Error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
            }
            finally
            {
                try
                {
                    System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
                    this.MONITOR.setMensajeRetenciones("[info] - Cerrando conexión con la Base de Datos... ");
                    ret.cambiaEstado(con,"EN ESPERA", 0);
                    con.desconectar();
                    System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
                    this.MONITOR.setMensajeRetenciones("[info] - Se ha cerrado la conexión con la base de datos");
                }
                catch(SQLException sqle)
                {
                    System.out.println("[error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                    this.MONITOR.setMensajeRetenciones("[Error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                }
                finally
                {
                    System.out.println("[info] - Continuando...");
                    this.MONITOR.setMensajeRetenciones("[info] - Continuando...");
                }
            }
        
            try{

                System.out.println("[info] - Pausando el Hilo Retenciones por 5 minuto(s)");
                this.MONITOR.setMensajeRetenciones("[info] - Pausando el Hilo Retenciones por 5 minuto(s)");

                this.sleep(300000);
                this.MONITOR.cambiaEstadoPanel("jPRetencion", "Retenciones [EN ESPERA]");
            } 
            catch (Exception ex)
            {
                System.out.println("[error] - Error al pausar el hilo");
                this.MONITOR.setMensajeRetenciones("[error] - Error al pausar el hilo");
            }
            finally
            {
                System.out.println("[info] - Continuando...");
                this.MONITOR.setMensajeRetenciones("[info] - Continuando...");
            }
        }
    
    }
    
    public ConexionBD getCONEXION() {
        return CONEXION;
    }

    public void setCONEXION(ConexionBD CONEXION) {
        this.CONEXION = CONEXION;
    }
    
    public frmMonitor getMONITOR() {
        return MONITOR;
    }

    public void setMONITOR(frmMonitor MONITOR) {
        this.MONITOR = MONITOR;
    }
      
}
