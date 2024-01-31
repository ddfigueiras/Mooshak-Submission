package aed.collections;
import java.util.Iterator;
import java.util.Stack;
public class Cordel implements Iterable<String> {
    private StringBuilder string;  // Para nós folha
    private int leftSize;   // Para nós de concatenação
    private Cordel left;     // Subárvore esquerda
    private Cordel right;
    public Cordel(String s) {
        if (s == null) {
            throw new IllegalArgumentException("A string não pode ser nula");
        }
        string = new StringBuilder(s);
        leftSize = s.length();
        left = null;
        right = null;
    }
    // Construtor para nós de concatenação
    public Cordel(Cordel left, Cordel right) {
        this.left = left;
        this.right = right;
        this.leftSize = left.length();
    }
    public int length() {
        if (string != null)
            return string.length();
        else
            return leftSize + (right != null ? right.length() : 0);
    }
    //Dado uma string, retorna um novo cordel que corresponde à concatenação à direita da string recebida com o cordel.
    public Cordel append(String s) {
        if (s == null) {
            return this;
        }
        return new Cordel(this, new Cordel(s));
    }

    //Dado um Cordel c, retorna um novo cordel que corresponde à concatenação à direita do cordel c recebido com o cordel.
    public Cordel append(Cordel c) {
        return new Cordel(this, c);
    }
    //Dado uma string, retorna um novo cordel que corresponde à concatenação à esquerda da string recebida com o cordel.
    public Cordel prepend(String s) {
        return new Cordel(new Cordel(s), this);
    }
    //Dado um Cordel c, retorna um novo cordel que corresponde à concatenação à esquerda do cordel c recebido com o cordel.
    public Cordel prepend(Cordel c) {
        return new Cordel(c, this);
    }
    //Imprime as strings guardadas dentro deste cordel, pela ordem da string mais à esquerda para a mais à direita.
    //Não imprime mudanças de linha.
    public void print() {
        if (string != null) {
            System.out.print(string);
        } else {
            left.print();
            right.print();
        }
    }
    public void printInfo() {
        if (string != null) {
            System.out.print("LEAF:\"" + string + "\"");
        } else {
            int leftLength = leftSize;
            int rightLength = (right != null) ? right.length() : 0;
            System.out.print("CONCAT[" + leftLength + "," + rightLength + "]:{");
            left.printInfo();
            System.out.print(",");
            if (right != null) {
                right.printInfo();
            }
            System.out.print("}");
        }
    }
    //Igual ao anterior, mas imprime uma nova linha no fim
    public void println() {
        print();
        System.out.println();
    }
    //Imprime informação sobre cada um dos nós da árvore.
    //igual ao anterior, mas imprime uma nova linha no fim
    public void printInfoNL() {
        this.printInfo();
        System.out.println();
    }
    //Dado um índice, devolve o caracter correspondente ao índice i do cordel.
    public char charAt(int i) {
        if (string != null) {
            return string.charAt(i);
        } else {
            if (i < leftSize) {
                return left.charAt(i);
            } else {
                return right.charAt(i - leftSize);
            }
        }
    }
    //Dado um índice, parte o cordel em dois cordéis no índice i. É devolvido um array com 2 cordéis.
    // O primeiro cordel contém as strings com todos os caracteres desde o índice 0 até o índice i-1,
    // e o segundo cordel contém as strings com todos os caracteres desde o índice i até ao fim.
    public Cordel[] split(int i)
    {
        if (string != null) {
            String s1 = string.substring(0, i);
            String s2 = string.substring(i);
            return new Cordel[] { new Cordel(s1), new Cordel(s2) };
        } else {
            if (i == leftSize) {
                return new Cordel[] { left, right };
            } else if (i < leftSize) {
                Cordel[] leftSplit = left.split(i);
                Cordel[] result = new Cordel[2];
                result[0] = leftSplit[0];
                result[1] = new Cordel(leftSplit[1], right);
                return result;
            } else {
                Cordel[] rightSplit = right.split(i - leftSize);
                Cordel[] result = new Cordel[2];
                result[0] = new Cordel(left, rightSplit[0]);
                result[1] = rightSplit[1];
                return result;
            }
        }
    }
    //Dado um índice, e uma string que não pode ser nula, retorna um cordel que corresponde ao
    // resultado de inserirmos a string s, na posição i do cordel.
    public Cordel insertAt(int i, String s) {
        if (s == null) {
            throw new IllegalArgumentException("A string não pode ser nula");
        }
        if (i == 0) {
            return new Cordel(s).append(this);
        } else if (i == length()) {
            return append(s);
        } else {
            Cordel[] splits = split(i);
            return splits[0].append(s).append(splits[1]);
        }
    }
    // Método para deletar caracteres a partir de uma posição específica
    public Cordel delete(int i, int length) {
        if (i < 0 || i >= length() || length < 0) {
            throw new IllegalArgumentException("Índice ou tamanho inválido");
        }
        if (i == 0) {
            Cordel[] rightSplit = split(length);
            return rightSplit[1];
        } else {
            Cordel[] leftSplit = split(i);
            Cordel[] rightSplit = leftSplit[1].split(length);
            return leftSplit[0].append(rightSplit[1]);
        }
    }
    //Devolve um iterador que vai percorrer todas as strings guardadas em nós folha deste cordel,
    // pela ordem correta (i.e. da esquerda para a direita).
    @Override
    public Iterator<String> iterator() {
        return new Iterator<String>() {
            private Stack<Cordel> stack = new Stack<>();
            private StringBuilder currentString = new StringBuilder();

            {
                stack.push(Cordel.this); // Push the root Cordel onto the stack
                updateCurrentString();
            }

            @Override
            public boolean hasNext() {
                return currentString.length() > 0;
            }

            @Override
            public String next() {
                String result = currentString.toString();
                updateCurrentString();
                return result;
            }

            private void updateCurrentString() {
                while (!stack.isEmpty()) {
                    Cordel current = stack.pop();

                    if (current.string != null) {
                        currentString = new StringBuilder(current.string);
                        return;
                    }

                    if (current.right != null) {
                        stack.push(current.right);
                    }

                    if (current.left != null) {
                        stack.push(current.left);
                    }
                }

                currentString.setLength(0); // Set length to 0 if there are no more elements
            }
        };
    }

    //Dado um índice i, e um tamanho, imprime um número de caracteres do cordel igual ao tamanho recebido,
    // a partir do índice i (inclusive).
    public void print(int i, int length) {
        if (i < 0 || i >= length() || length < 0) {
            throw new IllegalArgumentException("Índice ou tamanho inválido");
        }
        int currentIndex = i;
        while (length-- > 0) {
            System.out.print(charAt(currentIndex++));
        }
    }
    public void println(int i, int length) {
        print(i, length);
        System.out.println();
    }
    public static void main(String[] args)
    {
        Cordel cordel = new Cordel("Test");
        long startTime = System.nanoTime();

        for (int i = 0; i < 100000; i++) {
            cordel = cordel.append("1");
        }

        long endTime = System.nanoTime();
        long elapsedTimeMicros = (endTime - startTime) / 1000;

        System.out.println("Avg. time of append <= 15 microseconds: " + (elapsedTimeMicros <= 15000));
    }
}
