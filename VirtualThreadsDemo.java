import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Código de Demonstração da API de Virtual Threads (Java 20).
 *
 * Lembre-se de compilar e executar com --enable-preview.
 * javac --release 20 --enable-preview VirtualThreadsDemo.java
 * java --enable-preview VirtualThreadsDemo
 */
public class VirtualThreadsDemo {

    // Tarefa comum para nossos exemplos
    private static final Runnable simpleTask = () -> {
        Thread current = Thread.currentThread();
        System.out.printf("  [Tarefa] Olá da Thread: %s | ID: %d | É Virtual? %b | É Daemon? %b | Prioridade: %d\n",
                current.getName(),
                current.threadId(), // Usando threadId()
                current.isVirtual(), // Usando isVirtual()
                current.isDaemon(),  // Verificando se é Daemon
                current.getPriority() // Verificando Prioridade
        );
        try {
            // Usando sleep(Duration)
            Thread.sleep(Duration.ofMillis(50));
        } catch (InterruptedException e) {
            System.err.println("  [Tarefa] Thread " + current.getName() + " interrompida.");
            Thread.currentThread().interrupt();
        }
    };

    public static void main(String[] args) throws InterruptedException, ExecutionException {

        System.out.println("## 1. Como Criar Virtual Threads ##");
        System.out.println("=====================================\n");

        // --- 1.1 Thread.startVirtualThread ---
        System.out.println("1.1. Usando Thread.startVirtualThread:");
        Thread vtSimples = Thread.startVirtualThread(simpleTask);
        vtSimples.join(); // Espera terminar para continuar a demo
        System.out.println();

        // --- 1.2 Thread.Builder ---
        System.out.println("1.2. Usando Thread.Builder:");
        Thread.Builder builder = Thread.ofVirtual().name("MinhaVT-Builder-", 0);

        // Criando não iniciada
        Thread vtNaoIniciada = builder.unstarted(simpleTask);
        System.out.println("  -> Criada (unstarted): " + vtNaoIniciada.getName() + " | Estado: " + vtNaoIniciada.getState());
        vtNaoIniciada.start(); // Iniciando
        vtNaoIniciada.join();

        // Criando e iniciando diretamente
        Thread vtBuilderStart = builder.start(simpleTask);
        System.out.println("  -> Criada e Iniciada (start): " + vtBuilderStart.getName());
        vtBuilderStart.join();
        System.out.println();

        // --- 1.3 Executors.newVirtualThreadPerTaskExecutor ---
        System.out.println("1.3. Usando Executors.newVirtualThreadPerTaskExecutor:");
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<?> f1 = executor.submit(simpleTask);
            Future<?> f2 = executor.submit(simpleTask);
            f1.get(); // Espera tarefa 1
            f2.get(); // Espera tarefa 2
            System.out.println("  -> Tarefas via Executor concluídas.");
            // O executor é fechado automaticamente (AutoCloseable)
        }
        System.out.println();

        // --- 1.4 ThreadFactory ---
        System.out.println("1.4. Usando ThreadFactory:");
        ThreadFactory factory = Thread.ofVirtual().name("FabricaVT-", 0).factory();
        Thread vtDaFabrica = factory.newThread(simpleTask);
        System.out.println("  -> Thread criada pela fábrica: " + vtDaFabrica.getName());
        vtDaFabrica.start();
        vtDaFabrica.join();
        System.out.println();

        System.out.println("\n## 2. Introspecção e Debugging ##");
        System.out.println("=====================================\n");

        // --- 2.1 Thread.getAllStackTraces ---
        System.out.println("2.1. Thread.getAllStackTraces (Mostra apenas Platform Threads):");
        Map<Thread, StackTraceElement[]> platformThreads = Thread.getAllStackTraces();
        platformThreads.forEach((thread, stack) -> {
            if (!thread.isVirtual()) {
                System.out.printf("  -> Platform Thread encontrada: %s (ID: %d)\n", thread.getName(), thread.threadId());
            }
        });
        System.out.println();

        // --- 2.2 jcmd e JFR (Comentários) ---
        System.out.println("2.2. jcmd e JFR:");
        System.out.println("  -> Para Thread Dumps (no terminal): jcmd <pid> Thread.dump_to_file -format=json dump.json");
        System.out.println("  -> Use JFR para monitorar VTs, especialmente o evento 'jdk.VirtualThreadPinned'.");
        System.out.println();


        System.out.println("\n## 3. Boas Práticas (Exemplos Conceituais) ##");
        System.out.println("===============================================\n");

        // --- 3.1 Evitar Pinning (synchronized vs ReentrantLock) ---
        System.out.println("3.1. Risco de Pinning com 'synchronized':");
        Object lock = new Object();
        ReentrantLock reentrantLock = new ReentrantLock();

        Thread vtPinning = Thread.startVirtualThread(() -> {
            synchronized (lock) {
                System.out.println("  -> Dentro de 'synchronized'. Se houver I/O aqui, a VT **pina** a Carrier Thread.");
                // Se Thread.sleep() ou I/O ocorresse aqui, seria problemático.
            }
        });
        vtPinning.join();

        Thread vtNoPinning = Thread.startVirtualThread(() -> {
            reentrantLock.lock();
            try {
                System.out.println("  -> Dentro de 'ReentrantLock'. I/O aqui é mais seguro, não pina a Carrier Thread.");
            } finally {
                reentrantLock.unlock();
            }
        });
        vtNoPinning.join();
        System.out.println("  -> Prefira 'java.util.concurrent.locks' em vez de 'synchronized' com VTs.\n");

        // --- 3.2 ThreadLocal ---
        System.out.println("3.2. Cuidado com ThreadLocal:");
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        Thread vtThreadLocal = Thread.startVirtualThread(() -> {
            threadLocal.set("Dado da VT");
            System.out.println("  -> ThreadLocal funciona, mas use com moderação (risco de memória com muitas VTs).");
            // Lembre-se de limpar com threadLocal.remove() se o ciclo de vida for longo.
            threadLocal.remove();
        });
        vtThreadLocal.join();


        System.out.println("\n--- Fim da Demonstração ---");
    }
}