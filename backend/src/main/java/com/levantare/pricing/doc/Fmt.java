package com.levantare.pricing.doc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/** Indian style number / date formatting used inside the generated documents (7,88,000). */
public final class Fmt {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private Fmt() {}

    public static String date(LocalDate d) {
        return d == null ? "" : d.format(DATE);
    }

    public static String inr(long v) {
        String s = Long.toString(Math.abs(v));
        StringBuilder out = new StringBuilder();
        int len = s.length();
        if (len <= 3) {
            out.append(s);
        } else {
            out.append(s, 0, len - 3);
            String head = out.toString();
            out.setLength(0);
            for (int i = 0; i < head.length(); i++) {
                if (i > 0 && (head.length() - i) % 2 == 0) out.append(',');
                out.append(head.charAt(i));
            }
            out.append(',').append(s.substring(len - 3));
        }
        return (v < 0 ? "-" : "") + out;
    }

    /** Whole amounts without decimals, otherwise two decimals. */
    public static String inr(BigDecimal v) {
        BigDecimal r = v.setScale(2, RoundingMode.HALF_UP);
        long whole = r.abs().longValue();
        String s = inr(whole);
        int paise = r.abs().remainder(BigDecimal.ONE).movePointRight(2).intValue();
        if (paise != 0) s += String.format(".%02d", paise);
        return (r.signum() < 0 ? "-" : "") + s;
    }

    public static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }
}
