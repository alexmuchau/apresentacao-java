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
 * javac VirtualThreadsDemo.java
 * java VirtualThreadsDemo
 */
public class VirtualThreadsDemo {

    // Tarefa comum para nossos exemplos
    private static final Runnable simpleTask = () -> {
        Thread current = Thread.currentThread();
        String currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        System.out.printf("  [Tarefa] [%s] Olá da Thread: %s | ID: %d | É Virtual? %b | É Daemon? %b | Prioridade: %d\n",
            currentTime,
            current.getName(),
            current.threadId(),
            current.isVirtual(),
            current.isDaemon(),
            current.getPriority()
        );
        try {
            Thread.sleep(2000);
            currentTime = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            System.out.printf("  [Tarefa] [%s] Thread %s completou após 2 segundos.\n", currentTime, current.getName());
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
        vtNaoIniciada.start();
        System.out.println("  -> Iniciada (start): " + vtNaoIniciada.getName()  + " | Estado: " + vtNaoIniciada.getState());
        vtNaoIniciada.join();
        
        // Criando e iniciando diretamente
        System.out.println();
        Thread vtBuilderStart = builder.start(simpleTask);
        System.out.println("  -> Criada e Iniciada (start): " + vtBuilderStart.getName()  + " | Estado: " + vtNaoIniciada.getState());
        vtBuilderStart.join();
        
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
        Thread vt2DaFabrica = factory.newThread(simpleTask);
        System.out.println("  -> Thread criada pela fábrica: " + vtDaFabrica.getName());
        vtDaFabrica.start();
        vt2DaFabrica.start();
        
        try {
            vtDaFabrica.join();
            vt2DaFabrica.join();
        } catch (InterruptedException e) {
            System.err.println("Thread Main: Fui interrompida enquanto esperava pela thread trabalhadora.");
            Thread.currentThread().interrupt(); // Boa prática: restaurar o status de interrupção
        }
        
        // vtDaFabrica.join();
        System.out.println();

        System.out.println("\n--- Fim da Demonstração ---");
    }
}