package es.iescamas.multihilo.monitor;

/**
 * Clase que coordina la cena. Asegura que dos filósofos vecinos no coman a la
 * vez y gestiona los turnos de espera
 */
public final class MonitorFilosofos {

	public static final int PENSANDO = 0;
	public static final int HAMBRIENTO = 1;
	public static final int COMIENDO = 2;

	private final int n;

	// estado[idFilosofo] = PENSANDO / HAMBRIENTO / COMIENDO
	private final int[] estado;

	// tenedorPoseedor[idTenedor] = idFilosofo que lo tiene, o -1 si está libre
	private final int[] tenedorPoseedor;

	/**
	 * Prepara la mesa para el número de filósofos indicado. Todos empiezan pensando
	 * y los tenedores están libres.
	 * 
	 * @param n Número total de filósofos.
	 */
	public MonitorFilosofos(final int n) {
		if (n <= 1) {
			throw new IllegalArgumentException("Debe haber al menos dos filósofos");
		}

		this.n = n;
		this.estado = new int[n];
		this.tenedorPoseedor = new int[n];

		for (int id = 0; id < n; id++) {
			estado[id] = PENSANDO;
			tenedorPoseedor[id] = -1;
		}
	}

	// -----------------------------------------------------------
	// Cálculo de vecinos
	// -----------------------------------------------------------

	/** Calcula cuál es el tenedor de la izquierda. */
	private int tenedorIzquierdo(int idFilosofo) {
		return (idFilosofo - 1 + n) % n;
	}

	/** Calcula cuál es el tenedor de la derecha. */
	private int tenedorDerecho(int idFilosofo) {
		return idFilosofo; // por definición del modelo circular
	}

	/** Calcula quién es el vecino de la izquierda. */
	private int vecinoIzquierdo(int idFilosofo) {
		return (idFilosofo - 1 + n) % n;
	}

	/** Calcula quién es el vecino de la derecha. */
	private int vecinoDerecho(int idFilosofo) {
		return (idFilosofo + 1) % n;
	}

	// -----------------------------------------------------------
	// Lógica principal
	// -----------------------------------------------------------

	/**
	 * El filósofo intenta coger los tenedores para comer. 1. Se pone en estado
	 * HAMBRIENTO. 2. Intenta comer (mira si los vecinos le dejan). 3. Si no puede,
	 * se duerme (wait) hasta que le avisen.
	 * 
	 * @param idFilosofo El ID del filósofo.
	 */
	public synchronized void tomarTenedores(final int idFilosofo) throws InterruptedException {
		estado[idFilosofo] = HAMBRIENTO;
		intentarComer(idFilosofo);

		while (estado[idFilosofo] != COMIENDO) {
			wait();
		}
	}

	/**
	 * El filósofo suelta los tenedores y avisa a los demás. 1. Suelta sus dos
	 * tenedores. 2. Pasa a estado PENSANDO. 3. Comprueba si sus vecinos ahora
	 * pueden comer. 4. Despierta a todos (notifyAll) para que revisen si pueden
	 * comer.
	 * 
	 * @param idFilosofo El ID del filósofo.
	 */
	public synchronized void dejarTenedores(final int idFilosofo) {

		// libera sus tenedores primero
		liberarTenedor(tenedorIzquierdo(idFilosofo));
		liberarTenedor(tenedorDerecho(idFilosofo));

		estado[idFilosofo] = PENSANDO;

		// Permite que los vecinos intenten comer ahora
		intentarComer(vecinoIzquierdo(idFilosofo));
		intentarComer(vecinoDerecho(idFilosofo));

		notifyAll();
	}

	/**
	 * Comprueba si es posible empezar a comer. Solo come si: - Él tiene hambre. -
	 * Su vecino izquierdo NO está comiendo. - Su vecino derecho NO está comiendo.
	 */
	private void intentarComer(final int idFilosofo) {

		if (estado[idFilosofo] == HAMBRIENTO && estado[vecinoIzquierdo(idFilosofo)] != COMIENDO
				&& estado[vecinoDerecho(idFilosofo)] != COMIENDO) {

			estado[idFilosofo] = COMIENDO;

			ocuparTenedor(tenedorIzquierdo(idFilosofo), idFilosofo);
			ocuparTenedor(tenedorDerecho(idFilosofo), idFilosofo);
		}
	}

	// -----------------------------------------------------------
	// Gestión de tenedores
	// -----------------------------------------------------------

	/** Marca que un tenedor está ocupado por alguien. */
	private void ocuparTenedor(final int idTenedor, final int idFilosofo) {
		tenedorPoseedor[idTenedor] = idFilosofo;
	}

	/** Marca que un tenedor está libre (-1). */
	private void liberarTenedor(final int idTenedor) {
		tenedorPoseedor[idTenedor] = -1;
	}

	// -----------------------------------------------------------
	// Getters de consulta (todos synchronized)
	// -----------------------------------------------------------

	/** Devuelve en qué estado está un filósofo. */
	public synchronized int getEstado(final int idFilosofo) {
		return estado[idFilosofo];
	}

	/** Devuelve quién tiene un tenedor concreto. */
	public synchronized int getPoseedorTenedor(final int idTenedor) {
		return tenedorPoseedor[idTenedor];
	}

	public int getNumFilosofos() {
		return n;
	}
}
