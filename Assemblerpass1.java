import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;

class Tuple {
    String mnemonic, m_class, opcode;
    int length;

    Tuple() {}
    Tuple(String s1, String s2, String s3, String s4) {
        mnemonic = s1;
        m_class = s2;
        opcode = s3;
        length = Integer.parseInt(s4);
    }
}

class Symtuple {
    String symbol, address;
    int length;

    Symtuple(String s1, String s2, int i1) {
        symbol = s1;
        address = s2;
        length = i1;
    }
}

class Littuple {
    String literal, address;
    int length;

    Littuple(String s1, String s2, int i1) {
        literal = s1;
        address = s2;
        length = i1;
    }
}

public class Assemblerpass1 {
    static int lc, symtabptr = 0, littabptr = 0, pooltabptr = 0;
    static int pooltable[] = new int[10];
    static Map<String, Tuple> MOT;
    static Map<String, Symtuple> symtable;
    static ArrayList<Littuple> littable;
    static Map<String, String> regaddrTable;
    static PrintWriter out_pass1;
    static int line_no;

    public static void main(String[] args) throws Exception {
        initializeTables();
        pass1();
    }

    public static void pass1() throws Exception {
        BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream("input.txt")));
        out_pass1 = new PrintWriter(new FileWriter("output_pass1.txt"), true);
        PrintWriter out_symtable = new PrintWriter(new FileWriter("Symtable.txt"), true);
        PrintWriter out_littable = new PrintWriter(new FileWriter("Littable.txt"), true);
        PrintWriter out_pooltable = new PrintWriter(new FileWriter("Pooltable.txt"), true);

        String s;
        lc = 0;
        while ((s = input.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(s, " ", false);
            String s_ar[] = new String[st.countTokens()];
            for (int i = 0; i < s_ar.length; i++) {
                s_ar[i] = st.nextToken();
            }
            if (s_ar.length == 0) {
                continue;
            }
            int curindex = 0;
            if (s_ar.length == 3) {
                String label = s_ar[0];
                insertintoSymtab(label, lc + "");
                curindex = 1;
            }
            String curtoken = s_ar[curindex];
            Tuple curtuple = MOT.get(curtoken);
            String istr = "";
            if (curtuple.m_class.equalsIgnoreCase("IS")) {
                istr += lc + " (" + curtuple.m_class + "," + curtuple.opcode + ")";
                lc += curtuple.length;
                istr += processOperands(s_ar[curindex + 1]);
            } else if (curtuple.m_class.equalsIgnoreCase("AD")) {
                if (curtuple.mnemonic.equalsIgnoreCase("START")) {
                    istr += lc + " (" + curtuple.m_class + "," + curtuple.opcode + ")";
                    lc = Integer.parseInt(s_ar[curindex + 1]);
                    istr += "(C," + s_ar[curindex + 1] + ")";
                } else if (curtuple.mnemonic.equalsIgnoreCase("LTORG")) {
                    istr += processLTORG();
                } else if (curtuple.mnemonic.equalsIgnoreCase("END")) {
                    istr += lc + " (" + curtuple.m_class + "," + curtuple.opcode + ")";
                    istr += processLTORG();
                }
            } else if (curtuple.m_class.equalsIgnoreCase("DL")) {
                istr += lc + " (" + curtuple.m_class + "," + curtuple.opcode + ")";
                if (curtuple.mnemonic.equalsIgnoreCase("DS")) {
                    lc = Integer.parseInt(s_ar[curindex + 1]);
                } else if (curtuple.mnemonic.equalsIgnoreCase("DC")) {
                    lc += curtuple.length;
                }
                istr += "(C," + s_ar[curindex + 1] + ")";
            }
            out_pass1.println(istr);
        }
        out_pass1.flush();
        out_pass1.close();

        Symtuple tuple;
        Iterator<Symtuple> it = symtable.values().iterator();
        while (it.hasNext()) {
            tuple = it.next();
            out_symtable.println(tuple.symbol + "\t" + tuple.address);
        }
        out_symtable.flush();
        out_symtable.close();

        Littuple littuple;
        for (int i = 0; i < littable.size(); i++) {
            littuple = littable.get(i);
            out_littable.println(littuple.literal + "\t" + littuple.address);
        }
        out_littable.flush();
        out_littable.close();
    }

    static String processLTORG() {
        Littuple littuple;
        String istr = "";
        for (int i = pooltable[pooltabptr - 1]; i < littable.size(); i++) {
            littuple = littable.get(i);
            littuple.address = lc + "";
            istr += lc + " (DL,02) (C," + littuple.literal + ")";
            lc++;
        }
        pooltable[pooltabptr] = littabptr;
        pooltabptr++;
        return istr;
    }

    static String processOperands(String operands) {
        StringTokenizer st = new StringTokenizer(operands, ",", false);
        String s_ar[] = new String[st.countTokens()];
        for (int i = 0; i < s_ar.length; i++) {
            s_ar[i] = st.nextToken();
        }
        String istr = "", curToken;
        for (int i = 0; i < s_ar.length; i++) {
            curToken = s_ar[i];
            if (curToken.startsWith("=")) {
                String literal = curToken.substring(1);
                insertintoLittab(literal, "");
                istr += "(L," + (littabptr - 1) + ")";
            } else if (regaddrTable.containsKey(curToken)) {
                istr += "(RG," + regaddrTable.get(curToken) + ")";
            } else {
                insertintoSymtab(curToken, "");
                istr += "(S," + (symtabptr - 1) + ")";
            }
        }
        return istr;
    }

    static void insertintoSymtab(String symbol, String address) {
        if (symtable.containsKey(symbol)) {
            Symtuple s = symtable.get(symbol);
            s.address = address;
        } else {
            symtable.put(symbol, new Symtuple(symbol, address, 1));
        }
        symtabptr++;
    }

    static void insertintoLittab(String literal, String address) {
        littable.add(new Littuple(literal, address, 1));
        littabptr++;
    }

    static void initializeTables() throws Exception {
        symtable = new LinkedHashMap<>();
        littable = new ArrayList<>();
        regaddrTable = new HashMap<>();
        MOT = new HashMap<>();
        String s, mnemonic;
        BufferedReader br;
        br = new BufferedReader(new InputStreamReader(new FileInputStream("MOT.txt")));
        while ((s = br.readLine()) != null) {
            StringTokenizer st = new StringTokenizer(s, " ", false);
            mnemonic = st.nextToken();
            MOT.put(mnemonic, new Tuple(mnemonic, st.nextToken(), st.nextToken(), st.nextToken()));
        }
        br.close();
        regaddrTable.put("AREG", "1");
        regaddrTable.put("BREG", "2");
        regaddrTable.put("CREG", "3");
        regaddrTable.put("DREG", "4");
        pooltable[pooltabptr] = littabptr;
        pooltabptr++;
    }
}
