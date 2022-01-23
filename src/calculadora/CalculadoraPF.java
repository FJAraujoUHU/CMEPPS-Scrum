package calculadora;

import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author Francisco Javier Araujo Mendoza
 */
public class CalculadoraPF extends javax.swing.JFrame {

    //Constantes que indican el índice de la tabla
    public static final int TABLAENTRADAS = 0;
    public static final int TABLASALIDAS = 1;
    public static final int TABLACONSULTAS = 2;
    public static final int TABLAFLES = 3;
    public static final int TABLAFLIS = 4;

    //Constantes que indican la complejidad
    public static final int SIMPLE = 0;
    public static final int MEDIO = 1;
    public static final int COMPLEJO = 2;

    private static final int PESO_PFNA[][] = {
        {3, 4, 6}, //Peso entradas
        {4, 5, 7}, //Peso salidas
        {3, 4, 6}, //Peso consultas
        {5, 7, 10}, //Peso FLEs
        {7, 10, 15} //Peso FLIs
    };

    //Constantes para ISBSG
    private static final String[] esfuerzoStr = {"MF", "MR", "PC", "Multi", "3GL", "4GL", "GenAp", "Mantenimiento", "Nuevo", "MF-3GL", "MF-4GL", "MF-GenAp", "MR-3GL", "MR-4GL", "PC-3GL", "PC-4GL", "Multi-3GL", "Multi-4GL", "MF-3GL-Mantenimiento"};
    private static final double[][] esfuerzoC = {{49.02, .763}, {78.88, .646}, {48.9, .661}, {16.01, .865}, {54.65, .717}, {29.5, .758}, {68.11, .66}, {52.58, .683}, {39.05, .731}, {65.37, .705}, {52.09, .64}, {65.68, .692}, {126.3, .565}, {62.35, .694}, {60.46, .648}, {36.48, .694}, {19.82, .666}, {6.49, .983}, {83.27, .65}};
    private static final String[] duracionStr = {"PC", "Multi", "4GL", "Nuevo", "PC-4GL", "Multi-4GL", "PC-4GL-Nuevo", "Multi-4GL-Nuevo"};
    private static final double[][] duracionC = {{.503, .409}, {.679, .341}, {.578, .393}, {.739, .359}, {.348, .471}, {.366, .451}, {.25, 515}, {.24, .518}};

    //Variable que va almacenando los elementos por complejidad
    private int nElemsPF[][] = {
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0},
        {0, 0, 0}
    };

    private static final DecimalFormat redondeo = new DecimalFormat("#.###");

    int pfna = 0;
    int sva = 0;
    double pfa = 0;
    double fa = 0;

    /**
     * Creates new form CalculadoraPF
     */
    public CalculadoraPF() {
        initComponents();
        tablaFA.getModel().addTableModelListener(new cambioFA());
        actualizarElementos();
    }

    /**
     * Listener que aplica los cambios de ventanaModificar
     */
    private class cierreModificar extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {

            int tabla;
            int indice;
            int cerrada;
            Object[] datos;
            DefaultTableModel tm;

            if (e.getWindow() instanceof VentanaModificar) {
                VentanaModificar vm = (VentanaModificar) e.getWindow();
                tabla = vm.getTabla();
                datos = vm.getDatos();
                cerrada = vm.getCierre();
                indice = vm.getIndice();
            } else {
                VentanaModificarConsulta vm = (VentanaModificarConsulta) e.getWindow();
                tabla = vm.getTabla();
                datos = vm.getDatos();
                cerrada = vm.getCierre();
                indice = vm.getIndice();
            }

            if (cerrada == VentanaModificar.GUARDAR) {
                tm = (DefaultTableModel) getTabla(tabla).getModel();
                tm.removeRow(indice);
                tm.insertRow(indice, datos);
                actualizarElementos();
            }
            e.getWindow().dispose();
        }

    }

    private class cambioFA implements TableModelListener {

        @Override
        public void tableChanged(TableModelEvent t) {
            actualizarResultados();
        }
    }

    //No usar para consultas, necesita más datos
    private int complejidad(int nFich, int nDE, int tabla) {
        switch (tabla) {
            case TABLAENTRADAS:
                switch (nFich) {
                    case 0:
                    case 1:
                        return nDE < 16 ? SIMPLE : MEDIO;
                    case 2:
                        if (nDE < 5) {
                            return SIMPLE;
                        }
                        if (nDE < 16) {
                            return MEDIO;
                        }
                        return COMPLEJO;
                    default:
                        return nDE < 5 ? MEDIO : COMPLEJO;
                }
            case TABLASALIDAS:
                switch (nFich) {
                    case 0:
                    case 1:
                        return nDE < 20 ? SIMPLE : MEDIO;
                    case 2:
                    case 3:
                        if (nDE < 6) {
                            return SIMPLE;
                        }
                        if (nDE < 20) {
                            return MEDIO;
                        }
                        return COMPLEJO;
                    default:
                        return nDE < 6 ? MEDIO : COMPLEJO;
                }
            case TABLACONSULTAS:
                return -1;      //Necesita más datos;
            case TABLAFLES:
            case TABLAFLIS:
                int nReg = nFich;   //Por claridad de código
                if (nReg < 2) {
                    return nDE < 51 ? SIMPLE : MEDIO;
                }
                if (nReg < 6) {
                    if (nDE < 20) {
                        return SIMPLE;
                    }
                    if (nDE < 51) {
                        return MEDIO;
                    }
                    return COMPLEJO;
                }
                return nDE < 20 ? MEDIO : COMPLEJO;
            default:
                return -1;
        }
    }

    private int complejidadConsulta(int nFichEntrada, int nDEEntrada, int nFichSalida, int nDESalida) {
        int entrada = complejidad(nFichEntrada, nDEEntrada, TABLAENTRADAS);
        int salida = complejidad(nFichSalida, nDESalida, TABLASALIDAS);
        return entrada > salida ? entrada : salida;
    }

    private void actualizarElementos() {
        nElemsPF = new int[][]{
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0},
            {0, 0, 0}
        };
        DefaultTableModel tm;

        //Cálculo de Entradas
        tm = (DefaultTableModel) tablaEntradas.getModel();
        if (tm.getRowCount() > 1) { //Si hay elementos
            int nFich, nDE;
            for (int i = 0; i < tm.getRowCount() - 1; i++) {
                nFich = ((List) tm.getValueAt(i, 1)).size();
                nDE = ((List) tm.getValueAt(i, 2)).size();
                nElemsPF[TABLAENTRADAS][complejidad(nFich, nDE, TABLAENTRADAS)]++;
            }
        }

        //Cálculo de Salidas
        tm = (DefaultTableModel) tablaSalidas.getModel();
        if (tm.getRowCount() > 1) { //Si hay elementos
            int nFich, nDE;
            for (int i = 0; i < tm.getRowCount() - 1; i++) {
                nFich = ((List) tm.getValueAt(i, 1)).size();
                nDE = ((List) tm.getValueAt(i, 2)).size();
                nElemsPF[TABLASALIDAS][complejidad(nFich, nDE, TABLASALIDAS)]++;
            }
        }

        //Cálculo de Consultas
        tm = (DefaultTableModel) tablaConsultas.getModel();
        if (tm.getRowCount() > 1) { //Si hay elementos
            int nFichEntrada, nDEEntrada, nFichSalida, nDESalida;
            for (int i = 0; i < tm.getRowCount() - 1; i++) {
                nFichEntrada = ((List) tm.getValueAt(i, 1)).size();
                nDEEntrada = ((List) tm.getValueAt(i, 2)).size();
                nFichSalida = ((List) tm.getValueAt(i, 3)).size();
                nDESalida = ((List) tm.getValueAt(i, 4)).size();
                nElemsPF[TABLACONSULTAS][complejidadConsulta(nFichEntrada, nDEEntrada, nFichSalida, nDESalida)]++;
            }
        }

        //Cálculo de FLEntrada
        tm = (DefaultTableModel) tablaFLEs.getModel();
        if (tm.getRowCount() > 1) { //Si hay elementos
            int nReg, nDE;
            for (int i = 0; i < tm.getRowCount() - 1; i++) {
                nReg = ((List) tm.getValueAt(i, 1)).size();
                nDE = ((List) tm.getValueAt(i, 2)).size();
                nElemsPF[TABLAFLES][complejidad(nReg, nDE, TABLAFLES)]++;
            }
        }

        //Cálculo de FLESalida
        tm = (DefaultTableModel) tablaFLIs.getModel();
        if (tm.getRowCount() > 1) { //Si hay elementos
            int nReg, nDE;
            for (int i = 0; i < tm.getRowCount() - 1; i++) {
                nReg = ((List) tm.getValueAt(i, 1)).size();
                nDE = ((List) tm.getValueAt(i, 2)).size();
                nElemsPF[TABLAFLIS][complejidad(nReg, nDE, TABLAFLIS)]++;
            }
        }

        //Poblar la tabla de resultados
        tm = (DefaultTableModel) tablaPF.getModel();
        for (int i = 0; i < nElemsPF.length; i++) {
            int suma = 0;
            for (int j = 0; j < 3; j++) {
                int cantidad = nElemsPF[i][j];
                String valor = cantidad == 0 ? "" : "" + cantidad;
                suma += cantidad * PESO_PFNA[i][j];
                valor += " x " + PESO_PFNA[i][j];
                tm.setValueAt(valor, i, j + 1);
            }
            tm.setValueAt(suma, i, 4);
        }
        
        
        pfna = 0;              //Suma total de PFNAs
        for (int i = 0; i < 5; i++) {
            pfna += (Integer) tm.getValueAt(i, 4);
        }
        //tm.setValueAt(sumaTotal, 5, 4);
        boxPFNATot.setText("" + pfna);

        btnISBSG.setEnabled(pfna != 0);
        actualizarResultados();
    }

    //Sólo actualiza los resultados del ajuste
    private void actualizarResultados() {
        sva = 0;
        DefaultTableModel tm = (DefaultTableModel) tablaFA.getModel();
        for (int i = 0; i < tm.getRowCount(); i++) {
            sva += Integer.valueOf((String) tm.getValueAt(i, 1));
        }
        fa = 0.65 + (0.01 * sva);
        pfa = pfna * fa;

        boxSVA.setText(redondeo.format(sva));
        boxFA1.setText(redondeo.format(sva));
        boxFA2.setText(redondeo.format(fa));
        boxPFA1.setText("" + pfna);
        boxPFA2.setText(redondeo.format(fa));
        boxPFA3.setText(redondeo.format(pfa));

    }

    private JTable getTablaActiva() {
        return getTabla(tabsRegistro.getSelectedIndex());
    }

    private JTable getTabla(int tabla) {
        switch (tabla) {
            case TABLAENTRADAS:
                return tablaEntradas;
            case TABLASALIDAS:
                return tablaSalidas;
            case TABLACONSULTAS:
                return tablaConsultas;
            case TABLAFLES:
                return tablaFLEs;
            case TABLAFLIS:
                return tablaFLIs;
            default:
                return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        selectorFA = new javax.swing.JComboBox<>();
        tabsRegistro = new javax.swing.JTabbedPane();
        tabEntradas = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tablaEntradas = new javax.swing.JTable();
        tabSalidas = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tablaSalidas = new javax.swing.JTable();
        tabConsultas = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tablaConsultas = new javax.swing.JTable();
        tabFLE = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tablaFLEs = new javax.swing.JTable();
        tabFLI = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        tablaFLIs = new javax.swing.JTable();
        panelPF = new javax.swing.JPanel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tablaPF = new javax.swing.JTable();
        labelPFNATot = new javax.swing.JLabel();
        boxPFNATot = new javax.swing.JTextField();
        panelAjuste = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        tablaFA = new javax.swing.JTable();
        boxSVA = new javax.swing.JTextField();
        labelSVA = new javax.swing.JLabel();
        panelResultadosFA = new javax.swing.JPanel();
        labelFA1 = new javax.swing.JLabel();
        boxFA1 = new javax.swing.JTextField();
        labelFA2 = new javax.swing.JLabel();
        boxFA2 = new javax.swing.JTextField();
        labelPFA1 = new javax.swing.JLabel();
        boxPFA1 = new javax.swing.JTextField();
        labelPFA2 = new javax.swing.JLabel();
        boxPFA2 = new javax.swing.JTextField();
        labelPFA3 = new javax.swing.JLabel();
        boxPFA3 = new javax.swing.JTextField();
        btnISBSG = new javax.swing.JButton();

        selectorFA.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "0", "1", "2", "3", "4", "5" }));

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Calculadora de Puntos Función");

        tabsRegistro.setBorder(javax.swing.BorderFactory.createTitledBorder("Registro de elementos"));

        tabEntradas.setName(""); // NOI18N

        tablaEntradas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nueva entrada...", null, null}
            },
            new String [] {
                "Nombre", "Ficheros", "Datos elementales"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaEntradas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaEntradas.setShowGrid(false);
        tablaEntradas.setShowHorizontalLines(true);
        tablaEntradas.getTableHeader().setReorderingAllowed(false);
        tablaEntradas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tablaMousePressed(evt);
            }
        });
        tablaEntradas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tablaKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(tablaEntradas);

        javax.swing.GroupLayout tabEntradasLayout = new javax.swing.GroupLayout(tabEntradas);
        tabEntradas.setLayout(tabEntradasLayout);
        tabEntradasLayout.setHorizontalGroup(
            tabEntradasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
        );
        tabEntradasLayout.setVerticalGroup(
            tabEntradasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
        );

        tabsRegistro.addTab("Entradas", tabEntradas);

        tabSalidas.setName(""); // NOI18N

        tablaSalidas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nueva entrada...", null, null}
            },
            new String [] {
                "Nombre", "Ficheros", "Datos elementales"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaSalidas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaSalidas.setShowGrid(false);
        tablaSalidas.setShowHorizontalLines(true);
        tablaSalidas.getTableHeader().setReorderingAllowed(false);
        tablaSalidas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tablaMousePressed(evt);
            }
        });
        tablaSalidas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tablaKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(tablaSalidas);

        javax.swing.GroupLayout tabSalidasLayout = new javax.swing.GroupLayout(tabSalidas);
        tabSalidas.setLayout(tabSalidasLayout);
        tabSalidasLayout.setHorizontalGroup(
            tabSalidasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        tabSalidasLayout.setVerticalGroup(
            tabSalidasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
        );

        tabsRegistro.addTab("Salidas", tabSalidas);

        tabConsultas.setName(""); // NOI18N

        tablaConsultas.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nueva entrada...", null, null, null, null}
            },
            new String [] {
                "Nombre", "F In", "DE In", "F Out", "DE Out"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaConsultas.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaConsultas.setShowGrid(false);
        tablaConsultas.setShowHorizontalLines(true);
        tablaConsultas.getTableHeader().setReorderingAllowed(false);
        tablaConsultas.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tablaMousePressed(evt);
            }
        });
        tablaConsultas.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tablaKeyPressed(evt);
            }
        });
        jScrollPane3.setViewportView(tablaConsultas);

        javax.swing.GroupLayout tabConsultasLayout = new javax.swing.GroupLayout(tabConsultas);
        tabConsultas.setLayout(tabConsultasLayout);
        tabConsultasLayout.setHorizontalGroup(
            tabConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        tabConsultasLayout.setVerticalGroup(
            tabConsultasLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
        );

        tabsRegistro.addTab("Consultas", tabConsultas);

        tabFLE.setName(""); // NOI18N

        tablaFLEs.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nueva entrada...", null, null}
            },
            new String [] {
                "Nombre", "Registros", "Datos elementales"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaFLEs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaFLEs.setShowGrid(false);
        tablaFLEs.setShowHorizontalLines(true);
        tablaFLEs.getTableHeader().setReorderingAllowed(false);
        tablaFLEs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tablaMousePressed(evt);
            }
        });
        tablaFLEs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tablaKeyPressed(evt);
            }
        });
        jScrollPane4.setViewportView(tablaFLEs);

        javax.swing.GroupLayout tabFLELayout = new javax.swing.GroupLayout(tabFLE);
        tabFLE.setLayout(tabFLELayout);
        tabFLELayout.setHorizontalGroup(
            tabFLELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        tabFLELayout.setVerticalGroup(
            tabFLELayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
        );

        tabsRegistro.addTab("FLEs", tabFLE);

        tabFLI.setName(""); // NOI18N

        tablaFLIs.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nueva entrada...", null, null}
            },
            new String [] {
                "Nombre", "Registros", "Datos elementales"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaFLIs.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tablaFLIs.setShowGrid(false);
        tablaFLIs.setShowHorizontalLines(true);
        tablaFLIs.getTableHeader().setReorderingAllowed(false);
        tablaFLIs.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                tablaMousePressed(evt);
            }
        });
        tablaFLIs.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                tablaKeyPressed(evt);
            }
        });
        jScrollPane5.setViewportView(tablaFLIs);

        javax.swing.GroupLayout tabFLILayout = new javax.swing.GroupLayout(tabFLI);
        tabFLI.setLayout(tabFLILayout);
        tabFLILayout.setHorizontalGroup(
            tabFLILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.TRAILING)
        );
        tabFLILayout.setVerticalGroup(
            tabFLILayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 244, Short.MAX_VALUE)
        );

        tabsRegistro.addTab("FLIs", tabFLI);

        panelPF.setBorder(javax.swing.BorderFactory.createTitledBorder("Cálculo de PFNA"));

        tablaPF.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Nº Entradas", "0 x 3", "0 x 4", "0 x 6",  new Integer(0)},
                {"Nº Salidas", "0 x 4", "0 x 5", "0 x 7",  new Integer(0)},
                {"Nº Consultas", "0 x 3", "0 x 4", "0 x 6",  new Integer(0)},
                {"Nº FLEs", "0 x 5", "0 x 7", "0 x 10",  new Integer(0)},
                {"Nº FLIs", "0 x 7", "0 x 10", "0 x 15",  new Integer(0)}
            },
            new String [] {
                "Descripción", "Sencilla", "Media", "Compleja", "Total PFs"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaPF.setEnabled(false);
        tablaPF.setRowSelectionAllowed(false);
        tablaPF.getTableHeader().setReorderingAllowed(false);
        jScrollPane6.setViewportView(tablaPF);

        labelPFNATot.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPFNATot.setText("Total:");

        boxPFNATot.setEditable(false);
        boxPFNATot.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxPFNATot.setText("XXX");

        javax.swing.GroupLayout panelPFLayout = new javax.swing.GroupLayout(panelPF);
        panelPF.setLayout(panelPFLayout);
        panelPFLayout.setHorizontalGroup(
            panelPFLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane6)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelPFLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(labelPFNATot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(boxPFNATot, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        panelPFLayout.setVerticalGroup(
            panelPFLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelPFLayout.createSequentialGroup()
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelPFLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelPFNATot, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(boxPFNATot)))
        );

        panelAjuste.setBorder(javax.swing.BorderFactory.createTitledBorder("Ajuste"));

        tablaFA.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {"Comunicación de datos", "0"},
                {"Funciones distribuidas", "0"},
                {"Prestaciones", "0"},
                {"Gran uso de la configuración", "0"},
                {"Velocidad de las transacciones", "0"},
                {"Entrada de datos en línea", "0"},
                {"Diseño para la eficiencia del usuario final", "0"},
                {"Actualización de datos en línea", "0"},
                {"Complejidad del proceso lógico interno de la aplicación", "0"},
                {"Reusabilidad del código", "0"},
                {"Facilidad de instalación", "0"},
                {"Facilidad de operación", "0"},
                {"Localizaciones múltiples", "0"},
                {"Facilidad de cambios", "0"}
            },
            new String [] {
                "Atributos", "Influencia"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tablaFA.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_ALL_COLUMNS);
        tablaFA.setRowSelectionAllowed(false);
        tablaFA.getTableHeader().setReorderingAllowed(false);
        jScrollPane7.setViewportView(tablaFA);
        if (tablaFA.getColumnModel().getColumnCount() > 0) {
            tablaFA.getColumnModel().getColumn(0).setMinWidth(240);
            tablaFA.getColumnModel().getColumn(1).setResizable(false);
            tablaFA.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(selectorFA));
            tablaFA.getColumnModel().getColumn(1).setCellRenderer(null);
        }

        boxSVA.setEditable(false);
        boxSVA.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxSVA.setText("XXX");

        labelSVA.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelSVA.setText("FA totales:");

        panelResultadosFA.setBorder(javax.swing.BorderFactory.createTitledBorder("Resultados"));

        labelFA1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        labelFA1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelFA1.setText("FA = 0.65 + (0.01 *");

        boxFA1.setEditable(false);
        boxFA1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxFA1.setText("XXX");

        labelFA2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        labelFA2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelFA2.setText(") =");

        boxFA2.setEditable(false);
        boxFA2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxFA2.setText("XXXXX");

        labelPFA1.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        labelPFA1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPFA1.setText("PFA =");

        boxPFA1.setEditable(false);
        boxPFA1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxPFA1.setText("XXX");

        labelPFA2.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        labelPFA2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPFA2.setText("*");

        boxPFA2.setEditable(false);
        boxPFA2.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxPFA2.setText("XXX");

        labelPFA3.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        labelPFA3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        labelPFA3.setText("=");

        boxPFA3.setEditable(false);
        boxPFA3.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        boxPFA3.setText("XXXXX");

        btnISBSG.setText("Estimación ISBSG...");
        btnISBSG.setEnabled(false);
        btnISBSG.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnISBSGActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout panelResultadosFALayout = new javax.swing.GroupLayout(panelResultadosFA);
        panelResultadosFA.setLayout(panelResultadosFALayout);
        panelResultadosFALayout.setHorizontalGroup(
            panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelResultadosFALayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelResultadosFALayout.createSequentialGroup()
                        .addComponent(labelPFA1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boxPFA1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelPFA2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boxPFA2, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelPFA3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boxPFA3, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(panelResultadosFALayout.createSequentialGroup()
                        .addComponent(labelFA1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boxFA1, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(labelFA2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(boxFA2, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnISBSG)
                .addContainerGap())
        );
        panelResultadosFALayout.setVerticalGroup(
            panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelResultadosFALayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnISBSG, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelResultadosFALayout.createSequentialGroup()
                        .addGroup(panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelFA1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(boxFA1)
                            .addComponent(labelFA2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(boxFA2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelResultadosFALayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(labelPFA1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(boxPFA1)
                            .addComponent(labelPFA2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(boxPFA3)
                            .addComponent(labelPFA3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(boxPFA2))))
                .addContainerGap())
        );

        javax.swing.GroupLayout panelAjusteLayout = new javax.swing.GroupLayout(panelAjuste);
        panelAjuste.setLayout(panelAjusteLayout);
        panelAjusteLayout.setHorizontalGroup(
            panelAjusteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane7)
            .addGroup(panelAjusteLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(labelSVA)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(boxSVA, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addComponent(panelResultadosFA, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        panelAjusteLayout.setVerticalGroup(
            panelAjusteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelAjusteLayout.createSequentialGroup()
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelAjusteLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(labelSVA, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(boxSVA))
                .addGap(83, 83, 83)
                .addComponent(panelResultadosFA, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(panelPF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(tabsRegistro))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(panelAjuste, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelAjuste, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(tabsRegistro, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(panelPF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void tablaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tablaMousePressed
        JTable tabla = getTablaActiva();
        if (tabla != null) {
            DefaultTableModel tm = (DefaultTableModel) tabla.getModel();
            int seleccion = tabla.rowAtPoint(evt.getPoint());

            if (seleccion == tm.getRowCount() - 1) {    //Si se ha clicado añadir
                if (evt.getButton() == 1) {             //Si clic izquierdo
                    JFrame vm;
                    if (tabla == tablaConsultas) {
                        tm.insertRow(seleccion, new Object[]{"Nuevo dato", new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()});
                        vm = new VentanaModificarConsulta(tabsRegistro.getSelectedIndex(), seleccion);
                    } else {
                        tm.insertRow(seleccion, new Object[]{"Nuevo dato", new ArrayList<String>(), new ArrayList<String>()});
                        vm = new VentanaModificar(tabsRegistro.getSelectedIndex(), seleccion);
                    }
                    vm.addWindowListener(new cierreModificar());
                    vm.setLocationRelativeTo(this);
                    vm.setVisible(true);
                }
            } else {                                //Si se ha clicado otro elemento
                if (evt.getButton() == 1) {         //Si es con el clic izquierdo, modificar
                    JFrame vm;
                    if (tabla == tablaConsultas) {
                        vm = new VentanaModificarConsulta(tm.getDataVector().elementAt(seleccion), tabsRegistro.getSelectedIndex(), seleccion);
                    } else {
                        vm = new VentanaModificar(tm.getDataVector().elementAt(seleccion), tabsRegistro.getSelectedIndex(), seleccion);
                    }
                    vm.setLocationRelativeTo(this);
                    vm.addWindowListener(new cierreModificar());
                    vm.setVisible(true);
                } else {                            //Si se clica con otro botón, eliminar
                    tm.removeRow(seleccion);
                    actualizarElementos();
                }
            }
        }
    }//GEN-LAST:event_tablaMousePressed

    private void tablaKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_tablaKeyPressed
        JTable tabla = getTablaActiva();
        if (tabla != null) {
            int seleccion = tabla.getSelectedRow();
            DefaultTableModel tm = (DefaultTableModel) tabla.getModel();
            int tecla = evt.getKeyCode();

            if (seleccion == tm.getRowCount() - 1) {
                switch (tecla) {
                    case KeyEvent.VK_ENTER -> { //Actúa como si se hubiese clicado

                        JFrame vm;
                        if (tabla == tablaConsultas) {
                            tm.insertRow(seleccion, new Object[]{"Nuevo dato", new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>(), new ArrayList<String>()});
                            vm = new VentanaModificarConsulta(tabsRegistro.getSelectedIndex(), seleccion);
                        } else {
                            tm.insertRow(seleccion, new Object[]{"Nuevo dato", new ArrayList<String>(), new ArrayList<String>()});
                            vm = new VentanaModificar(tabsRegistro.getSelectedIndex(), seleccion);
                        }
                        vm.setLocationRelativeTo(this);
                        vm.addWindowListener(new cierreModificar());
                        vm.setVisible(true);
                    }

                    case KeyEvent.VK_DELETE -> { //Borra la lista entera
                        while (tm.getRowCount() > 1) {
                            tm.removeRow(0);
                        }
                        actualizarElementos();
                    }
                }
            } else if (seleccion != -1) {
                switch (tecla) {
                    case KeyEvent.VK_ENTER -> { //Actúa como si se hubiese clicado
                        JFrame vm;
                        if (tabla == tablaConsultas) {
                            vm = new VentanaModificarConsulta(tm.getDataVector().elementAt(seleccion), tabsRegistro.getSelectedIndex(), seleccion);
                        } else {
                            vm = new VentanaModificar(tm.getDataVector().elementAt(seleccion), tabsRegistro.getSelectedIndex(), seleccion);
                        }
                        vm.setLocationRelativeTo(this);
                        vm.addWindowListener(new cierreModificar());
                        vm.setVisible(true);
                    }

                    case KeyEvent.VK_DELETE -> { //Borra el elemento
                        tm.removeRow(seleccion);
                        actualizarElementos();
                    }
                }
            }
        }

    }//GEN-LAST:event_tablaKeyPressed

    private void btnISBSGActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnISBSGActionPerformed

        if (pfna == 0) {
            JOptionPane.showMessageDialog(this, "Rellena los campos primero", "Advertencia", JOptionPane.WARNING_MESSAGE);
        } else {
            
            int indEsfuerzo;
            int indDuracion;
            
            JComboBox combo = new JComboBox(esfuerzoStr);
            JOptionPane.showMessageDialog(this, combo, "Selecciona características (Esfuerzo)", JOptionPane.QUESTION_MESSAGE);
            indEsfuerzo = combo.getSelectedIndex();
            
            if (Arrays.asList(duracionStr).contains((String) combo.getSelectedItem())) {
                //Si la característica está presente en la duración, saltar selección
                indDuracion = Arrays.asList(duracionStr).indexOf(combo.getSelectedItem());
            } else {
                combo = new JComboBox(duracionStr);
                JOptionPane.showMessageDialog(this, combo, "Selecciona características (Duración)", JOptionPane.QUESTION_MESSAGE);
                indDuracion = combo.getSelectedIndex();                
            }
            
            String output = "---Esfuerzo---\n" +
                    "Características: " + esfuerzoStr[indEsfuerzo] + "\n" +
                    "Esfuerzo = " + esfuerzoC[indEsfuerzo][0] + "*" + redondeo.format(pfa) + "^" + esfuerzoC[indEsfuerzo][1] + "=" +  redondeo.format(esfuerzoC[indEsfuerzo][0] * (Math.pow(pfa, esfuerzoC[indEsfuerzo][1]))) + "\n" +
                    "---Duración---\n" +
                    "Características: " + duracionStr[indDuracion] + "\n" +
                    "Esfuerzo = " + duracionC[indDuracion][0] + "*" + redondeo.format(pfa) + "^" + duracionC[indDuracion][1] + "=" +  redondeo.format(duracionC[indDuracion][0] * (Math.pow(pfa, duracionC[indDuracion][1])));
            JOptionPane.showMessageDialog(this, output, "Estimación ISBSG", JOptionPane.PLAIN_MESSAGE);
        }
    }//GEN-LAST:event_btnISBSGActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the native OS' look and feel */
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException | UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(CalculadoraPF.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            CalculadoraPF c = new CalculadoraPF();
            c.setLocationRelativeTo(null);
            c.setVisible(true);
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField boxFA1;
    private javax.swing.JTextField boxFA2;
    private javax.swing.JTextField boxPFA1;
    private javax.swing.JTextField boxPFA2;
    private javax.swing.JTextField boxPFA3;
    private javax.swing.JTextField boxPFNATot;
    private javax.swing.JTextField boxSVA;
    private javax.swing.JButton btnISBSG;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JLabel labelFA1;
    private javax.swing.JLabel labelFA2;
    private javax.swing.JLabel labelPFA1;
    private javax.swing.JLabel labelPFA2;
    private javax.swing.JLabel labelPFA3;
    private javax.swing.JLabel labelPFNATot;
    private javax.swing.JLabel labelSVA;
    private javax.swing.JPanel panelAjuste;
    private javax.swing.JPanel panelPF;
    private javax.swing.JPanel panelResultadosFA;
    private javax.swing.JComboBox<String> selectorFA;
    private javax.swing.JPanel tabConsultas;
    private javax.swing.JPanel tabEntradas;
    private javax.swing.JPanel tabFLE;
    private javax.swing.JPanel tabFLI;
    private javax.swing.JPanel tabSalidas;
    private javax.swing.JTable tablaConsultas;
    private javax.swing.JTable tablaEntradas;
    private javax.swing.JTable tablaFA;
    private javax.swing.JTable tablaFLEs;
    private javax.swing.JTable tablaFLIs;
    private javax.swing.JTable tablaPF;
    private javax.swing.JTable tablaSalidas;
    private javax.swing.JTabbedPane tabsRegistro;
    // End of variables declaration//GEN-END:variables
}
