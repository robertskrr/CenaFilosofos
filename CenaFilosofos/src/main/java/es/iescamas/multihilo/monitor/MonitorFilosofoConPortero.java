package es.iescamas.multihilo.monitor;

import java.util.concurrent.Semaphore;

/**
 * Esta clase añade un "Portero" (Semáforo) al problema. Su función principal es
 * evitar el Deadlock limitando el número de filósofos que pueden sentarse a la
 * vez. Si hay N sillas, solo deja sentarse a N-1 personas. Así garantiza que
 * siempre quede un sitio libre y la cadena de bloqueo se rompa.
 */
public final class MonitorFilosofoConPortero {

	private final MonitorFilosofos monitor;
	private final Semaphore portero;
	private final int maxSentados;

	// NUEVO: quién está esperando al portero
	private final boolean[] esperandoPortero;

	/**
	 * Crea un portero que permite sentarse a (Total - 1) filósofos.
	 * 
	 * @param monitor El gestor de los tenedores.
	 */
	public MonitorFilosofoConPortero(final MonitorFilosofos monitor) {
		this(monitor, Math.max(1, monitor.getNumFilosofos() - 1));
	}

	/**
	 * Constructor con un límite personalizado.
	 * 
	 * @param monitor     El gestor de los tenedores.
	 * @param maxSentados Número máximo de sillas disponibles.
	 */
	public MonitorFilosofoConPortero(final MonitorFilosofos monitor, final int maxSentados) {
		if (monitor == null) {
			throw new IllegalArgumentException("El monitor de filósofos no puede ser null");
		}
		final int n = monitor.getNumFilosofos();
		if (maxSentados < 1 || maxSentados > n) {
			throw new IllegalArgumentException("maxSentados debe estar entre 1 y " + n + ", recibido: " + maxSentados);
		}
		this.monitor = monitor;
		this.maxSentados = maxSentados;
		this.portero = new Semaphore(maxSentados, true);
		this.esperandoPortero = new boolean[n];
	}

	public int getMaxSentados() {
		return maxSentados;
	}

	// ========= NUEVOS MÉTODOS DE CONSULTA PARA UTILIDAD =========

	public int getNumFilosofos() {
		return monitor.getNumFilosofos();
	}

	/** true si el filósofo está esperando turno en el portero (footman). */
	public synchronized boolean isEsperandoPortero(final int idFilosofo) {
		return esperandoPortero[idFilosofo];
	}

	// ============================================================

	/**
	 * El filósofo intenta entrar al comedor y coger tenedores. 1. Pide permiso al
	 * portero (`portero.acquire()`). 2. Si hay sitio, pasa y llama al monitor para
	 * coger tenedores. 3. Si no hay sitio, se queda bloqueado aquí esperando.
	 * 
	 * @param idFilosofo Quién quiere sentarse.
	 */
	public void sentarse(final int idFilosofo) throws InterruptedException {
		validarFilosofo(idFilosofo);

		// Marca que está esperando al portero
		setEsperandoPortero(idFilosofo, true);

		portero.acquire(); // bloquea si ya hay maxSentados sentados
		boolean exito = false;
		try {
			monitor.tomarTenedores(idFilosofo);
			exito = true;
		} finally {
			// Pase lo que pase, ya no está esperando al portero
			setEsperandoPortero(idFilosofo, false);

			if (!exito) {
				portero.release();
			}
		}
	}

	/**
	 * El filósofo termina de comer, suelta los tenedores y se va. 1. Deja los
	 * tenedores en el monitor. 2. Avisa al portero ("release") de que queda una
	 * silla libre.
	 * 
	 * @param idFilosofo Quién se levanta.
	 */
	public void levantarse(final int idFilosofo) {
		validarFilosofo(idFilosofo);
		monitor.dejarTenedores(idFilosofo);
		portero.release();
	}

	/** Marca internamente si alguien está esperando */
	private synchronized void setEsperandoPortero(final int idFilosofo, final boolean valor) {
		esperandoPortero[idFilosofo] = valor;
	}

	/** Comprueba que el ID del filósofo sea correcto. */
	private void validarFilosofo(final int idFilosofo) {
		final int n = monitor.getNumFilosofos();
		if (idFilosofo < 0 || idFilosofo >= n) {
			throw new IllegalArgumentException(
					"Índice de filósofo fuera de rango: " + idFilosofo + " (debe estar entre 0 y " + (n - 1) + ")");
		}
	}
}
