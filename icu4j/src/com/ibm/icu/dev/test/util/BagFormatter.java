/*
 *******************************************************************************
 * Copyright (C) 2002-2003, International Business Machines Corporation and         *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 *
 * $Source: /xsrl/Nsvn/icu/icu4j/src/com/ibm/icu/dev/test/util/BagFormatter.java,v $
 * $Date: 2004/02/07 00:59:26 $
 * $Revision: 1.6 $
 *
 *****************************************************************************************
 */
package com.ibm.icu.dev.test.util;

import com.ibm.icu.text.*;
import com.ibm.icu.lang.*;
import com.ibm.icu.impl.*;

import java.io.*;
import java.util.*;
//import java.util.regex.*;
import java.text.MessageFormat;

public class BagFormatter {
    
    public static final Transliterator toHTML = Transliterator.createFromRules(
        "any-html",        
            "'<' > '&lt;' ;" +
            "'&' > '&amp;' ;" +
            "'>' > '&gt;' ;" +
            "'\"' > '&quot;' ; ",
        Transliterator.FORWARD);
        
    public static final Transliterator fromHTML = Transliterator.createFromRules(
        "html-any",        
            "'<' < '&'[lL][Tt]';' ;" +
            "'&' < '&'[aA][mM][pP]';' ;" +
            "'>' < '&'[gG][tT]';' ;" +
            "'\"' < '&'[qQ][uU][oO][tT]';' ; ",
        Transliterator.REVERSE);

    public static final PrintWriter CONSOLE = new PrintWriter(System.out,true);

    private static PrintWriter log = CONSOLE;
    
    private boolean abbreviated = false;
    
    /**
     * Compare two UnicodeSets, and show the differences
     * @param name1 name of first set to be compared
     * @param set1 first set
     * @param name2 name of second set to be compared
     * @param set2 second set
     * @return formatted string
     */
    public String showSetDifferences(
        String name1,
        UnicodeSet set1,
        String name2,
        UnicodeSet set2) {
            
        StringWriter result = new StringWriter();
        showSetDifferences(new PrintWriter(result),name1,set1,name2,set2);
        result.flush();
        return result.getBuffer().toString();
    }
    
    public String showSetDifferences(
        String name1,
        Collection set1,
        String name2,
        Collection set2) {
            
        StringWriter result = new StringWriter();
        showSetDifferences(new PrintWriter(result), name1, set1, name2, set2);
        result.flush();
        return result.getBuffer().toString();
    }

    /**
     * Compare two UnicodeSets, and show the differences
     * @param name1 name of first set to be compared
     * @param set1 first set
     * @param name2 name of second set to be compared
     * @param set2 second set
     * @return formatted string
     */
    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        UnicodeSet set1,
        String name2,
        UnicodeSet set2) {
        if (pw == null) pw = CONSOLE;
        String[] names = { name1, name2 };

        UnicodeSet temp = new UnicodeSet(set1).removeAll(set2);
        pw.println();
        showSetNames(pw, inOut.format(names), temp);

        temp = new UnicodeSet(set2).removeAll(set1);
        pw.println();
        showSetNames(pw, outIn.format(names), temp);

        temp = new UnicodeSet(set2).retainAll(set1);
        pw.println();
        showSetNames(pw, inIn.format(names), temp);
    }
    
    public void showSetDifferences(
        PrintWriter pw,
        String name1,
        Collection set1,
        String name2,
        Collection set2) {
            
        if (pw == null) pw = CONSOLE;
        String[] names = { name1, name2 };
        // damn'd collection doesn't have a clone, so
        // we go with Set, even though that
        // may not preserve order and duplicates
        Collection temp = new HashSet(set1);
        temp.removeAll(set2);
        pw.println();
        showSetNames(pw, inOut.format(names), temp);

        temp.clear();
        temp.addAll(set2);
        temp.removeAll(set1);
        pw.println();
        showSetNames(pw, outIn.format(names), temp);

        temp.clear();
        temp.addAll(set1);
        temp.retainAll(set2);
        pw.println();
        showSetNames(pw, inIn.format(names), temp);
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @param separator to be placed between any strings
     * @return
     * @internal
     */
    public String showSetNames(String title, Object c) {
        StringWriter buffer = new StringWriter();
        PrintWriter output = new PrintWriter(buffer);
        output.println(title);
        mainVisitor.doAt(c, output);
        return buffer.toString();
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @param separator to be placed between any strings
     * @return
     * @internal
     */
    public void showSetNames(PrintWriter output, String title, Object c) {
        output.println(title);
        mainVisitor.doAt(c, output);
        output.flush();
    }

    /**
     * Returns a list of items in the collection, with each separated by the separator.
     * Each item must not be null; its toString() is called for a printable representation
     * @param c source collection
     * @param separator to be placed between any strings
     * @return
     * @internal
     */
    public void showSetNames(String filename, String title, Object c) throws IOException {
        PrintWriter pw = new PrintWriter(
            new OutputStreamWriter(
                new FileOutputStream(filename),"utf-8"));
        showSetNames(log,title,c);
        pw.close();
    }
    
    public String getAbbreviatedName(
        String source,
        String pattern,
        String substitute) {
            
        int matchEnd = NameIterator.findMatchingEnd(source, pattern);
        int sdiv = source.length() - matchEnd;
        int pdiv = pattern.length() - matchEnd;
        StringBuffer result = new StringBuffer();
        addMatching(
            source.substring(0, sdiv),
            pattern.substring(0, pdiv),
            substitute,
            result);
        addMatching(
            source.substring(sdiv),
            pattern.substring(pdiv),
            substitute,
            result);
        return result.toString();
    }

    abstract public static class Relation {
        abstract public String getRelation(String a, String b);
    }
    
    static class NullRelation extends Relation {
        public String getRelation(String a, String b) { return ""; }
    }
    
    private Relation r = new NullRelation();
   
    public BagFormatter setRelation(Relation r) {
        this.r = r;
        return this; // for chaining
    }
    
    public Relation getRelation() {
        return r;
    }
            
    /*
     r.getRelati on(last, s) + quote(s) + "\t#" + UnicodeSetFormatter.getResolvedName(s)
    */
    /*
    static final UnicodeSet NO_NAME =
        new UnicodeSet("[\\u0080\\u0081\\u0084\\u0099\\p{Cn}\\p{Co}]");
    static final UnicodeSet HAS_NAME = new UnicodeSet(NO_NAME).complement();
    static final UnicodeSet NAME_CHARACTERS =
        new UnicodeSet("[A-Za-z0-9\\<\\>\\-\\ ]");

    public UnicodeSet getSetForName(String namePattern) {
        UnicodeSet result = new UnicodeSet();
        Matcher m = Pattern.compile(namePattern).matcher("");
        // check for no-name items, and add in bulk
        m.reset("<no name>");
        if (m.matches()) {
            result.addAll(NO_NAME);
        }
        // check all others
        UnicodeSetIterator usi = new UnicodeSetIterator(HAS_NAME);
        while (usi.next()) {
            String name = getName(usi.codepoint);
            if (name == null)
                continue;
            m.reset(name);
            if (m.matches()) {
                result.add(usi.codepoint);
            }
        }
        // Note: if Regex had some API so that if we could tell that
        // an initial substring couldn't match, e.g. "CJK IDEOGRAPH-"
        // then we could optimize by skipping whole swathes of characters
        return result;
    }
    */
    
    public BagFormatter setMergeRanges(boolean in) {
        mergeRanges = in;
        return this;
    }
    public BagFormatter setShowSetAlso(boolean b) {
        showSetAlso = b;
        return this;
    }
    
    /*public String getName(int codePoint) {
        return getName(codePoint, false);
    }*/
    
    public String getName(String separator, int start, int end) {
        if (nameSource == null || nameSource == UnicodeProperty.NULL) return "";
        String result = getName(start, false);
        if (start == end) return separator + result;
        String endString = getName(end, false);
        if (abbreviated) endString = getAbbreviatedName(endString,result,"~");
        return separator + result + ".." + endString;
    }
    
    public String getName(String s) {
        return getName(s, false);
    }
    
    UnicodeLabel nameSource;
    
    static class NameLabel extends UnicodeLabel {
        UnicodeProperty nameProp;
        UnicodeProperty name1Prop;
        UnicodeProperty catProp;
        //UnicodeProperty shortCatProp;
        
        NameLabel(UnicodeProperty.Factory source) {
            nameProp = source.getProperty("Name");
            name1Prop = source.getProperty("Unicode_1_Name");
            catProp = source.getProperty("General_Category");
            //shortCatProp = source.getProperty("General_Category");
        }       

        public String getValue(int codePoint, boolean isShort) {
            String hcp = !isShort
                ? "U+" + Utility.hex(codePoint, 4) + " "
                : "";
            String result = nameProp.getValue(codePoint);
            if (result != null)
                return hcp + result;
            String prop = catProp.getValue(codePoint, true);
            if (prop.equals("Control")) {
                result = name1Prop.getValue(codePoint);
                if (result != null)
                    return hcp + "<" + result + ">";
            }
            return hcp + "<reserved>";
        }
        
    }
    
    // refactored
    public String getName(int codePoint, boolean withCodePoint) {
        return nameSource.getValue(codePoint, !withCodePoint);
    }

    public String getName(String s, boolean withCodePoint) {
        return nameSource.getValue(s, separator, !withCodePoint);
    }
    
    public String hex(String s) {
        return UnicodeLabel.HEX.getValue(s, separator, true);
    }
    
    public String hex(int start, int end) {
        String s = Utility.hex(start,4);
        if (start == end) return s;
        return s + ".." + Utility.hex(end,4);
    }
    
    private String separator = ",";
    private String prefix = "[";
    private String suffix = "]";
    UnicodeProperty.Factory source;
    UnicodeLabel labelSource = UnicodeLabel.NULL;
    UnicodeLabel valueSource = UnicodeLabel.NULL;
    private boolean showCount = true;

    public BagFormatter setUnicodePropertySource(UnicodeProperty.Factory source) {
        this.source = source;
        nameSource = new NameLabel(source);
        return this;
    }
    
    {
        setUnicodePropertySource(ICUPropertyFactory.make());
        Map labelMap = new HashMap();
        labelMap.put("Lo","L&");
        labelMap.put("Lu","L&");
        labelMap.put("Lt","L&");
        setLabelSource(new UnicodeProperty.FilteredUnicodeProperty(
            source.getProperty("General_Category"),
                new UnicodeProperty.MapFilter(labelMap)));
    }
    
    public String join(Object o) {
        return labelVisitor.join(o);
    }

    // ===== PRIVATES =====
    
    private Join labelVisitor = new Join();
    
    private boolean mergeRanges = true;
    private Transliterator showLiteral = null;
    private boolean showSetAlso = false;

    private RangeFinder rf = new RangeFinder();

    private MessageFormat inOut = new MessageFormat("In {0}, but not in {1}:");
    private MessageFormat outIn = new MessageFormat("Not in {0}, but in {1}:");
    private MessageFormat inIn = new MessageFormat("In both {0}, and in {1}:");

    private MyVisitor mainVisitor = new MyVisitor();

    /*
    private String getLabels(int start, int end) {
        Set names = new TreeSet();
        for (int cp = start; cp <= end; ++cp) {
            names.add(getLabel(cp));
        }
        return labelVisitor.join(names);
    }
    */

    private void addMatching(
        String source,
        String pattern,
        String substitute,
        StringBuffer result) {
        NameIterator n1 = new NameIterator(source);
        NameIterator n2 = new NameIterator(pattern);
        boolean first = true;
        while (true) {
            String s1 = n1.next();
            if (s1 == null)
                break;
            String s2 = n2.next();
            if (!first)
                result.append(" ");
            first = false;
            if (s1.equals(s2))
                result.append(substitute);
            else
                result.append(s1);
        }
    }

    private static NumberFormat nf =
        NumberFormat.getIntegerInstance(Locale.ENGLISH);
    
    private String lineSeparator = "\r\n";

    private class MyVisitor extends Visitor {
        private PrintWriter output;
        Tabber.MonoTabber myTabber;
        String commentSeparator = "\t# ";
        
        public void doAt(Object c, PrintWriter output) {
            this.output = output;
            myTabber = new Tabber.MonoTabber();
            int valueSize = valueSource.getMaxWidth(shortValue);
            if (valueSize > 0) valueSize += 2;
            if (!mergeRanges) {
                myTabber.add(0,Tabber.LEFT);
                myTabber.add(6 + valueSize,Tabber.LEFT);
                myTabber.add(2 + labelSource.getMaxWidth(shortLabel),Tabber.LEFT);
                myTabber.add(4,Tabber.LEFT);
            } else {
                myTabber.add(0,Tabber.LEFT);
                myTabber.add(15 + valueSize,Tabber.LEFT);
                myTabber.add(2 + labelSource.getMaxWidth(shortLabel),Tabber.LEFT);
                myTabber.add(11,Tabber.LEFT);
                myTabber.add(7,Tabber.LEFT);
            }
            commentSeparator = (showCount || showLiteral != null 
              || labelSource != UnicodeProperty.NULL || nameSource != UnicodeProperty.NULL) 
            ? "\t# " : "";
            doAt(c);
        }

        public String format(Object o) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            doAt(o);
            pw.flush();
            String result = sw.getBuffer().toString();
            pw.close();
            return result;
        }

        protected void doBefore(Object container, Object o) {
            if (showSetAlso && container instanceof UnicodeSet) {
                output.print("# " + container + lineSeparator);
            }
        }

        protected void doBetween(Object container, Object lastItem, Object nextItem) {
        }

        protected void doAfter(Object container, Object o) {
            output.print("# Total: " + nf.format(count(container)) + lineSeparator);
        }

        protected void doSimpleAt(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry oo = (Map.Entry)o;
                Object key = oo.getKey();
                Object value = oo.getValue();
                doBefore(o, key);
                doAt(key);
                output.print("->");
                doAt(value);
                doAfter(o, value);
            } else if (o instanceof Visitor.CodePointRange) {
                doAt((Visitor.CodePointRange) o);
            } else {
                String thing = o.toString();
                output.print(
                    myTabber.process(
                        hex(thing)
                            + commentSeparator
                            + insertLiteral(thing)
                            + "\t"
                            + getName(thing))
                    + lineSeparator);
            }
        }

        protected void doAt(Visitor.CodePointRange usi) {
            if (!mergeRanges) {
                for (int cp = usi.codepoint; cp <= usi.codepointEnd; ++cp) {
                    String label = labelSource.getValue(cp, shortLabel);
                    if (label.length() != 0)
                        label += " ";
                    String value = valueSource.getValue(cp, shortValue);
                    if (value.length() != 0) {
                        value = "; " + value;
                    }
                    output.print(
                        myTabber.process(
                            Utility.hex(cp, 4)
                                + value
                                + commentSeparator
                                + label
                                + insertLiteral(cp,cp)
                                + getName("\t", cp, cp))
                        + lineSeparator);
                }
            } else {
                rf.reset(usi.codepoint, usi.codepointEnd + 1);
                while (rf.next()) {
                    /*
                    String label = (usi.codepoint != usi.codepointEnd) 
                        ? label = getLabels(usi.codepoint, usi.codepointEnd) 
                        : getLabel(usi.codepoint);
                    */
                    int start = rf.start;
                    int end = rf.limit - 1;
                    String label = rf.label;
                    if (label.length() != 0)
                        label += " ";
                    String value = rf.value;
                    if (value.length() != 0) {
                        value = "; " + value;
                    }
                    String count = showCount ? "\t["+ nf.format(end - start + 1)+ "]" : "";
                    output.print(
                        myTabber.process(
                            hex(start, end)
                                + value
                                + commentSeparator
                                + label
                                + count
                                + insertLiteral(start, end)
                                + getName("\t", start, end))
                        + lineSeparator);
                }
            }
        }

        private String insertLiteral(String thing) {
            return (showLiteral == null ? ""
                :  " \t(" + showLiteral.transliterate(thing) + ") ");
        }

        private String insertLiteral(int start, int end) {
            return (showLiteral == null ? "" :
                " \t(" + showLiteral.transliterate(UTF16.valueOf(start))
                        + ((start != end)
                            ? (".." + showLiteral.transliterate(UTF16.valueOf(end)))
                            : "")
                + ") ");
        }
        /*
        private String insertLiteral(int cp) {
            return (showLiteral == null ? ""
                :  " \t(" + showLiteral.transliterate(UTF16.valueOf(cp)) + ") ");
        }
        */
    }

    /**
     * Iterate through a string, breaking at words.
     * @author Davis
     */
    private static class NameIterator {
        String source;
        int position;
        int start;
        int limit;

        NameIterator(String source) {
            this.source = source;
            this.start = 0;
            this.limit = source.length();
        }
        /** 
         * Find next word, including trailing spaces
         * @return
         */
        String next() {
            if (position >= limit)
                return null;
            int pos = source.indexOf(' ', position);
            if (pos < 0 || pos >= limit)
                pos = limit;
            String result = source.substring(position, pos);
            position = pos + 1;
            return result;
        }

        static int findMatchingEnd(String s1, String s2) {
            int i = s1.length();
            int j = s2.length();
            try {
                while (true) {
                    --i; // decrement both before calling function!
                    --j;
                    if (s1.charAt(i) != s2.charAt(j))
                        break;
                }
            } catch (Exception e) {} // run off start

            ++i; // counteract increment
            i = s1.indexOf(' ', i); // move forward to space
            if (i < 0)
                return 0;
            return s1.length() - i;
        }
    }

    private class RangeFinder {
        int start, limit;
        private int veryLimit;
        String label, value;
        void reset(int start, int limit) {
            this.limit = start;
            this.veryLimit = limit;
        }
        boolean next() {
            if (limit >= veryLimit)
                return false;
            start = limit; // set to end of last
            label = labelSource.getValue(limit, shortLabel);
            value = valueSource.getValue(limit, shortLabel);
            limit++;
            for (; limit < veryLimit; limit++) {
                String s = labelSource.getValue(limit, shortLabel);
                String v = valueSource.getValue(limit, shortLabel);
                if (!s.equals(label) || !v.equals(value)) break;
            }
            // at this point, limit is the first item that has a different label than source
            // OR, we got to the end, and limit == veryLimit
            return true;
        }
    }

    boolean shortLabel = true;
    boolean shortValue = true;

    public String getPrefix() {
        return prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public BagFormatter setPrefix(String string) {
        prefix = string;
        return this;
    }

    public BagFormatter setSuffix(String string) {
        suffix = string;
        return this;
    }

    public boolean isAbbreviated() {
        return abbreviated;
    }

    public BagFormatter setAbbreviated(boolean b) {
        abbreviated = b;
        return this;
    }

    public UnicodeProperty.Factory getSource() {
        return source;
    }

    public UnicodeLabel getLabelSource() {
        return labelSource;
    }

    /**
     * @deprecated
     */
    public static void addAll(UnicodeSet source, Collection target) {
        source.addAllTo(target);
    }
    
    // UTILITIES
    
    public static final Transliterator hex = Transliterator.getInstance(
        "[^\\u0009\\u0020-\\u007E\\u00A0-\\u00FF] hex");
    
    public static BufferedReader openUTF8Reader(String dir, String filename) throws IOException {
        return openReader(dir,filename,"UTF-8");
    }
        
    public static BufferedReader openReader(String dir, String filename, String encoding) throws IOException {
        File file = new File(dir + filename);
        if (log != null) {
            log.println("Opening File: " 
                + file.getCanonicalPath());
        }
        return new BufferedReader(
            new InputStreamReader(
                new FileInputStream(file),
                encoding),
            4*1024);       
    }
    
    public static PrintWriter openUTF8Writer(String dir, String filename) throws IOException {
        return openWriter(dir,filename,"UTF-8");
    }
        
    public static PrintWriter openWriter(String dir, String filename, String encoding) throws IOException {
        File file = new File(dir + filename);
        if (log != null) {
            log.println("Creating File: " 
                + file.getCanonicalPath());
        }
        //File parent = new File(file.getParent());
        //parent.mkdirs();
        return new PrintWriter(
            new BufferedWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    encoding),
                4*1024));       
    }
    public static PrintWriter getLog() {
        return log;
    }
    public BagFormatter setLog(PrintWriter writer) {
        log = writer;
        return this;
    }
    public String getSeparator() {
        return separator;
    }
    public BagFormatter setSeparator(String string) {
        separator = string;
        return this;
    }
    public Transliterator getShowLiteral() {
        return showLiteral;
    }
    public BagFormatter setShowLiteral(Transliterator transliterator) {
        showLiteral = transliterator;
        return this;
    }
    
    // ===== CONVENIENCES =====
    private class Join extends Visitor {
        StringBuffer output = new StringBuffer();
        int depth = 0;
        String join (Object o) {
            output.setLength(0);
            doAt(o);
            return output.toString();
        }
        protected void doBefore(Object container, Object item) {
            ++depth;
            output.append(prefix);
        }
        protected void doAfter(Object container, Object item) {
            output.append(suffix);
            --depth;
        }
        protected void doBetween(Object container, Object lastItem, Object nextItem) {
            output.append(separator);
        }
        protected void doSimpleAt(Object o) {
            if (o != null) output.append(o.toString());
        }
    }
    /**
     * @return
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * @param string
     */
    public void setLineSeparator(String string) {
        lineSeparator = string;
    }

    /**
     * @param label
     */
    public BagFormatter setLabelSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        labelSource = label;
        return this;
    }

    /**
     * @return
     */
    public UnicodeLabel getNameSource() {
        return nameSource;
    }

    /**
     * @param label
     */
    public BagFormatter setNameSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        nameSource = label;
        return this;
    }

    /**
     * @return
     */
    public UnicodeLabel getValueSource() {
        return valueSource;
    }

    /**
     * @param label
     */
    public BagFormatter setValueSource(UnicodeLabel label) {
        if (label == null) label = UnicodeLabel.NULL;
        valueSource = label;
        return this;
    }

    /**
     * @return
     */
    public boolean isShowCount() {
        return showCount;
    }

    /**
     * @param b
     */
    public BagFormatter setShowCount(boolean b) {
        showCount = b;
        return this;
    }

}