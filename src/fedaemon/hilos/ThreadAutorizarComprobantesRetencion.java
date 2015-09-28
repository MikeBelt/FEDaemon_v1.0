
package fedaemon.hilos;

import fedaemon.util.ConexionBD;
import fedaemon.dao.RetencionDAO;
import fedaemon.frms.frmMonitor;
import java.sql.SQLException;

/**
 *
 * @author Mike
 */
public final class ThreadAutorizarComprobantesRetencion extends Thread{
    
    protected ConexionBD conexionBD;
    protected frmMonitor frmMonitor;
    public RetencionDAO retencionDAO;
    
    @Override
    public void run(){
//    ConexionBD conexionBD=new ConexionBD("inter","INTER2014","192.168.1.245","GLTEVCOL");
//    ConexionBD conexionBD=new ConexionBD("sistemas","tevsur","192.168.15.200","GLTEVSUR");
       int enviadas=0;
       int contar=0;
       ConexionBD con=new ConexionBD(conexionBD.getUsr(),conexionBD.getPass(),conexionBD.getServer(),conexionBD.getBase());
       retencionDAO=new RetencionDAO();
       retencionDAO.setMonitor(frmMonitor);
    
        System.out.println("[info] - Iniciando hilo para autorización de Retenciones... ");
        this.frmMonitor.setMensajeRetenciones("[info] - Iniciando hilo para autorización de Retenciones... ");            
        while(true)
        {
            this.frmMonitor.cambiaEstadoPanel("jPRetencion", "Retenciones [EJECUTANDO]");
            this.frmMonitor.limpiaRetenciones();
            try
            {
                System.out.println("[info] - Estableciendo conexión con la Base de Datos... ");
                this.frmMonitor.setMensajeRetenciones("[info] - Estableciendo conexión con la Base de Datos... ");
                con.conectar();

                System.out.println("[info] - Conexión para hilo retenciones exitosa");
                this.frmMonitor.setMensajeRetenciones("[info] - Conexión para hilo retenciones exitosa");

                System.out.println("[info] - Verificando Comprobantes de Retención pendientes de autorización...");
                this.frmMonitor.setMensajeRetenciones("[info] - Verificando Comprobantes de Retención pendientes de autorización...");
        
                contar=retencionDAO.consultarRetencionPendientes(con,"07",frmMonitor.getServicio().getAmbiente());
                retencionDAO.cambiaEstado(con, "EJECUTANDO", contar);
                
                if(contar==0)
                { 
                    System.out.println("[info] - No hay retenciones pendientes de autorización.");
                    this.frmMonitor.setMensajeRetenciones("[info] - No hay retenciones pendientes de autorización.");

                }
                else
                {
                    System.out.println("[info] - Petición de autorización para: "+contar+" retenciones");
                    this.frmMonitor.setMensajeRetenciones("[info] - Petición de autorización para: "+contar+" retenciones");
                    enviadas=retencionDAO.enviarRetenciones(con);
                    System.out.println("[info] - Se han enviado: "+enviadas+" comprobantes de retención para autorización del SRI.");
                    this.frmMonitor.setMensajeRetenciones("[info] - Se han enviado: "+enviadas+" comprobantes de retención para autorización del SRI.");

                }
            }
            catch(SQLException sqlex)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
                this.frmMonitor.setMensajeRetenciones("[Error] - Error al conectar con la base de datos\n"+sqlex.getMessage());
            }
            catch(ClassNotFoundException cnfe)
            {
                System.out.println("[error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
                this.frmMonitor.setMensajeRetenciones("[Error] - Error al conectar con la base de datos\n"+cnfe.getMessage());
            }
            finally
            {
                try
                {
                    System.out.println("[info] - Cerrando conexión con la Base de Datos... ");
                    this.frmMonitor.setMensajeRetenciones("[info] - Cerrando conexión con la Base de Datos... ");
                    retencionDAO.cambiaEstado(con,"EN ESPERA", 0);
                    con.desconectar();
                    System.out.println("[info] - Se ha cerrado la conexión con la base de datos");
                    this.frmMonitor.setMensajeRetenciones("[info] - Se ha cerrado la conexión con la base de datos");
                }
                catch(SQLException sqle)
                {
                    System.out.println("[error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                    this.frmMonitor.setMensajeRetenciones("[Error] - Error al cerrar la conexión con la base de datos\n"+sqle.getMessage());
                }
                finally
                {
                    System.out.println("[info] - Continuando...");
                    this.frmMonitor.setMensajeRetenciones("[info] - Continuando...");
                }
            }
        
            try{

                System.out.println("[info] - Pausando el Hilo Retenciones por 5 minuto(s)");
                this.frmMonitor.setMensajeRetenciones("[info] - Pausando el Hilo Retenciones por 5 minuto(s)");

                this.sleep(300000);
                this.frmMonitor.cambiaEstadoPanel("jPRetencion", "Retenciones [EN ESPERA]");
            } 
            catch (Exception ex)
            {
                System.out.println("[error] - Error al pausar el hilo");
                this.frmMonitor.setMensajeRetenciones("[error] - Error al pausar el hilo");
            }
            finally
            {
                System.out.println("[info] - Continuando...");
                this.frmMonitor.setMensajeRetenciones("[info] - Continuando...");
            }
        }
    
    }
    
    public ConexionBD getConexion() {
        return conexionBD;
    }

    public void setConexion(ConexionBD conexion) {
        this.conexionBD = conexion;
    }
    
    public frmMonitor getMonitor() {
        return frmMonitor;
    }

    public void setMonitor(frmMonitor MONITOR) {
        this.frmMonitor = MONITOR;
    }
      
}
