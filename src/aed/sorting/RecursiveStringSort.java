package aed.sorting;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Array;
import java.util.*;

//podem alterar esta classe, se quiserem
class Limits
{
    char minChar;
    char maxChar;
    int maxLength;
}

public class RecursiveStringSort extends Sort
{
    private static final Random R = new Random();


    //esta implementação base do quicksort é fornecida para que possam comparar o tempo de execução do quicksort
    //com a vossa implementação do RecursiveStringSort
    public static <T extends Comparable<T>> void quicksort(T[] a)
    {
        qsort(a, 0, a.length-1);
    }

    private static <T extends Comparable<T>> void qsort(T[] a, int low, int high)
    {
        if (high <= low) return;
        int j = partition(a, low, high);
        qsort(a, low, j-1);
        qsort(a, j+1, high);
    }

    private static <T extends Comparable<T>> int partition(T[] a, int low, int high)
    {
        //partition into a[low...j-1],a[j],[aj+1...high] and return j
        //choose a random pivot
        int pivotIndex = low + R.nextInt(high+1-low);
        exchange(a,low,pivotIndex);
        T v = a[low];
        int i = low, j = high +1;

        while(true)
        {
            while(less(a[++i],v)) if(i == high) break;
            while(less(v,a[--j])) if(j == low) break;

            if(i >= j) break;
            exchange(a , i, j);
        }
        exchange(a, low, j);

        return j;
    }



    //método de ordenação insertionSort
    //no entanto este método recebe uma Lista de Strings em vez de um Array de Strings
    public static void insertionSort(List<String> a)
    {
        int n = a.size();
        for(int i = 1; i < n; i++){
            String key = a.get(i);
            int before = i - 1;
            while (before >= 0 && a.get(before).compareTo(key) > 0){
                a.set(before + 1, a.get(before));
                before--;
            }
            a.set(before + 1, key);
        }

    }
    // 0 - banana, 1- alberto

    public static Limits determineLimits(List<String> a, int characterIndex)
    {

        Limits limits = new Limits();
        if (a.isEmpty()) {
            limits.minChar = Character.MIN_VALUE;
        } else {
            limits.minChar = Character.MAX_VALUE;
        }
        for (String current : a) {
            if (current.length() > characterIndex) {
                char currentChar = current.charAt(characterIndex);
                limits.minChar = (currentChar < limits.minChar) ? currentChar : limits.minChar;
                limits.maxChar = (currentChar > limits.maxChar) ? currentChar : limits.maxChar;

            }
            else limits.minChar = 0;
            limits.maxLength = Math.max(current.length(), limits.maxLength);
        }

        return limits;
    }

    //ponto de entrada principal para o vosso algoritmo de ordenação
    public static void sort(String[] a) {
        recursive_sort(Arrays.asList(a),0);
    }
    public static void recursive_sort(List<String> a, int characterIndex)
    {
        if (a.size() < 70)
            insertionSort(a);
        else
        {
            Limits limitBuckets = determineLimits(a, characterIndex);
            char min = limitBuckets.minChar;
            char max = limitBuckets.maxChar;
            List<List<String>> buckets = new ArrayList<>();
            for (int i = min; i <= max; i++) {
                buckets.add(new ArrayList<>());
            }
            for (String current : a) {
                char currentChar = (characterIndex < current.length()) ? current.charAt(characterIndex) : 0;
                int bucketIndex = currentChar - min;
                buckets.get(bucketIndex).add(current);
            }
            int index = 0;
            for (List<String> balde : buckets) {
                Collections.sort(balde);
                for (String s : balde) {
                    a.set(index++, s);
                }
            }
        }
    }

    public static void fasterSort(String[] a)
    {
        List<String> b = Arrays.asList(a);
        if (b.size() < 70)
            insertionSort(b);
        else {
            Map<Character, List<String>> buckets = new HashMap<>();

            for (String current : a) {
                char currentChar = (!current.isEmpty()) ? current.charAt(0) : 0;
                buckets.computeIfAbsent(currentChar, k -> new ArrayList<>()).add(current);
            }

            List<Character> sortedKeys = new ArrayList<>(buckets.keySet());
            Collections.sort(sortedKeys);

            int index = 0;
            for (Character key : sortedKeys) {
                List<String> bucket = buckets.get(key);
                if (!bucket.isEmpty()) {
                    Collections.sort(bucket);
                    for (String s : bucket) {
                        b.set(index++, s);
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        TestRecursiveStringSort.testeDois();
    }

}

class TestRecursiveStringSort
{
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static void testeUm()
    {
        int[] arraySizes = {10, 100, 1000, 10000, 100000, 1000000, 200000000, 8000, 16000};

        for (int size : arraySizes)
        {
            String[] randomStrings = generateRandomStrings(size);

            long quicksortTime = measureTime(() -> RecursiveStringSort.quicksort(randomStrings.clone()));
            long sortTime = measureTime(() -> RecursiveStringSort.sort(randomStrings.clone()));

            System.out.println("Array Size: " + size);
            System.out.println("Quicksort Time: " + quicksortTime + " ms");
            System.out.println("Sort Time: " + sortTime + " ms");
            System.out.println();
        }
    }
    public static void testeDois() {
        int[] arraySizes = {100000, 200000, 400000, 800000, 160000, 320000, 100000, 100000};
        System.out.println("Teste 2 // sort");
        for (int size : arraySizes) {

            String[] randomStrings = generateRandomStrings(size);

            long beforeMemory = getUsedMemory();
            RecursiveStringSort.sort(randomStrings);
            long afterMemory = getUsedMemory();

            long memoryUsed = afterMemory - beforeMemory;

            System.out.println("Array Size: " + size);
            System.out.println("Memory Used: " + memoryUsed + " bytes");
            System.out.println();
        }
    }
    public static void testeTres() {
        int[] arraySizes = {100000, 200000, 400000, 800000, 160000, 320000, 100000, 10000000};
        System.out.println("Teste 3 // fasterSort");
        for (int size : arraySizes) {

            String[] randomStrings = generateRandomStrings(size);

            long beforeMemory = getUsedMemory();
            RecursiveStringSort.fasterSort(randomStrings);
            long afterMemory = getUsedMemory();

            long memoryUsed = afterMemory - beforeMemory;

            System.out.println("Array Size: " + size);
            System.out.println("Memory Used: " + memoryUsed + " bytes");
            System.out.println();
        }
    }
    private static String[] generateRandomStrings(int size) {
        String[] array = new String[size];
        Random random = new Random();

        for (int i = 0; i < size; i++) {
            int length = random.nextInt(20) + 1;
            array[i] = generateRandomString(length);
        }

        return array;
    }

    private static String generateRandomString(int length) {

        if (length <= 0) {
            throw new IllegalArgumentException("O comprimento tem q ser + q 0.");
        }

        Random random = new Random();
        StringBuilder randomStringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            randomStringBuilder.append(randomChar);
        }

        return randomStringBuilder.toString();
    }

    private static long measureTime(Runnable task) {
        long startTime = System.currentTimeMillis();
        task.run();
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }
    private static long getUsedMemory() {
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        return memoryMXBean.getHeapMemoryUsage().getUsed();
    }
}


/*
Este relatório visa analisar e comparar os métodos de ordenação quicksort e sort implementados na classe RecursiveStringSort.

Vamos então à geração de testes…

Geração de Arrays:
Tamanhos => 10, 100, 1000, 10000, 100000 e 1000000 elementos
Elementos dos arrays => strings aleatórias de tamanhos variados (entre 1 e 20 caracteres).

PRINT (RELATORIO)

Evidencias:
Ambos os métodos mostraram bom desempenho para tamanhos pequenos de array.
Quando o tamanho aumenta, o método sort demonstrou ser mais eficiente.

O método sort superou o quicksort em desempenho quando o tamanho do array ultrapassou aproximadamente 1000 elementos. É mais eficiente para arrays de tamanhos médios/grandes.

======================================================================

Análise da complexidade espacial do método sort:

PRINT (RELATORIO)

Observa-se um aumento no consumo de memória à medida que o tamanho do array aumenta.
Parece haver uma relação linear ou quase linear entre o tamanho do array e o consumo de memória.
Analise temporal:
O(n^2) no pior caso (por causa da ordenação por inserção) que ocorre se a lista estiver invertida.

======================================================================
FastSort em comparação com outros:

PRINT (RELATORIO)

Em suma…
Quicksort:
Eficiente em geral.
Divide a lista em partições menores, ordena essas partições e combina os resultados.
Boa performance em média e pior caso.
Rápido para listas de médio e grande tamanho.
Pode ser ligeiramente menos eficiente em listas pequenas devido à sobrecarga de chamadas recursivas.

Recursive Sort:
Troca para a ordenação por inserção para listas pequenas.
Divide a lista em "buckets" e ordena esses "buckets" individualmente.
Eficiente para listas pequenas..
Melhora o desempenho em casos específicos.
Mau para listas grandes.

FasterSort:
Potencialmente mais eficiente para alguns conjuntos de dados específicos.
Dependente do comportamento dos dados e dos critérios de ordenação. Pode se tornar bastante rápido porque tem mais critérios (especificações), mais “preciso”.
======================================================================
Pontos positivos:

Quicksort: Eficiente para listas de tamanho médio a grande.
Recursive Sort: Ideal para listas pequenas.
FasterSort: Mais útil quando sabemos mais indicações.
.
Pontos negativos:

Quicksort: Desempenho ligeiramente inferior em listas muito pequenas.
Recursive Sort: Mais eficiente para listas grandes..
FasterSort: Depende de mais coisas.



*/