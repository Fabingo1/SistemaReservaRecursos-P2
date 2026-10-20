package modelo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class Reserva implements Serializable {
    private String id;
    private String actividad;
    private Date fecha;
    private Date horaInicio;
    private Date horaFin;
    private EstadoReserva estado;
    private Funcionario funcionario;
    private List<DetalleReserva> detalles;

    public Reserva() {
        this.detalles = new ArrayList<>();
        this.estado = EstadoReserva.ACTIVA;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getActividad() {
        return actividad;
    }

    public void setActividad(String actividad) {
        this.actividad = actividad;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public Date getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(Date horaInicio) {
        this.horaInicio = horaInicio;
    }

    public Date getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(Date horaFin) {
        this.horaFin = horaFin;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public void setEstado(EstadoReserva estado) {
        this.estado = estado;
    }

    public Funcionario getFuncionario() {
        return funcionario;
    }

    public void setFuncionario(Funcionario funcionario) {
        this.funcionario = funcionario;
    }

    public List<DetalleReserva> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleReserva> detalles) {
        this.detalles = detalles;
    }


    public void cancelar() {
        this.estado = EstadoReserva.CANCELADA;
    }

    public boolean estaActiva() {
        return this.estado == EstadoReserva.ACTIVA;
    }

    public boolean perteneceA(Funcionario funcionario) {
        return this.funcionario != null
                && funcionario != null
                && this.funcionario.getId().equals(funcionario.getId());
    }

    // Nombres sin prefijo "get" porque así XMLEncoder no los trata como propiedades a persistir
    public int minutoInicio() {
        return minutosDelDia(horaInicio);
    }

    public int minutoFin() {
        return minutosDelDia(horaFin);
    }

    public Date calcularMomentoInicio() {
        return combinar(fecha, horaInicio);
    }

    public Date calcularMomentoFin() {
        return combinar(fecha, horaFin);
    }

    public boolean yaPaso() {
        Date fin = calcularMomentoFin();
        return fin != null && fin.before(new Date());
    }

    public boolean esDelDia(Date dia) {
        return mismoDia(fecha, dia);
    }

    /** Única regla de solapamiento por hora que comparten las matrices de Calendarización y Actividades. */
    public boolean ocupaHora(Date dia, int hora) {
        if (fecha == null || horaInicio == null || horaFin == null || !esDelDia(dia)) {
            return false;
        }
        int inicioFranja = hora * 60;
        int finFranja = (hora + 1) * 60;
        return minutoInicio() < finFranja && minutoFin() > inicioFranja;
    }

    public boolean seSuperponeCon(Date dia, Date inicio, Date fin) {
        if (!esDelDia(dia) || horaInicio == null || horaFin == null) {
            return false;
        }
        return minutoInicio() < minutosDelDia(fin) && minutosDelDia(inicio) < minutoFin();
    }

    //utilidades estáticas reutilizables
    public static int minutosDelDia(Date hora) {
        if (hora == null) return 0;
        Calendar cal = Calendar.getInstance();
        cal.setTime(hora);
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    public static boolean mismoDia(Date a, Date b) {
        if (a == null || b == null) return false;
        Calendar ca = Calendar.getInstance();
        ca.setTime(a);
        Calendar cb = Calendar.getInstance();
        cb.setTime(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    /** Toma año/mes/día de dia y hora/minuto de hora. */
    public static Date combinar(Date dia, Date hora) {
        if (dia == null || hora == null) return null;
        Calendar cDia = Calendar.getInstance();
        cDia.setTime(dia);
        Calendar cHora = Calendar.getInstance();
        cHora.setTime(hora);
        cDia.set(Calendar.HOUR_OF_DAY, cHora.get(Calendar.HOUR_OF_DAY));
        cDia.set(Calendar.MINUTE, cHora.get(Calendar.MINUTE));
        cDia.set(Calendar.SECOND, 0);
        cDia.set(Calendar.MILLISECOND, 0);
        return cDia.getTime();
    }
}
