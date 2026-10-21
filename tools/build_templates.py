"""Builds the two Word templates (software / services proposal) from the sample
.doc files by replacing customer specific text with {{tokens}}.

Usage:  python3 tools/build_templates.py            (needs python-docx + LibreOffice)

Token conventions understood by the backend DocumentGenerator:
  {{name}}                 simple value (multi-line values become line breaks)
  {{list.field}}           a table row containing these tokens is repeated for every
                           element of `list` (costRows, phases)
  {{?hasSoftware}}         row is removed when the flag is false
"""
import copy, re, subprocess, sys, tempfile, shutil
from pathlib import Path
import docx
from docx.oxml.ns import qn

ROOT = Path(__file__).resolve().parent.parent
SAMPLES = ROOT / "docs" / "samples"
OUT = ROOT / "backend" / "src" / "main" / "resources" / "csa-templates"


def to_docx(doc_file, tmp):
    subprocess.run(["soffice", "--headless", "--convert-to", "docx", "--outdir", tmp, str(doc_file)],
                   check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
    return Path(tmp) / (Path(doc_file).stem + ".docx")


def all_paragraphs(d):
    seen = set()
    def walk(parent_paras, tables):
        for p in parent_paras:
            if p._p not in seen:
                seen.add(p._p); yield p
        for t in tables:
            for r in t.rows:
                for c in r.cells:
                    yield from walk(c.paragraphs, c.tables)
    yield from walk(d.paragraphs, d.tables)
    for s in d.sections:
        yield from walk(s.header.paragraphs, [])
        yield from walk(s.footer.paragraphs, [])


def replace_in_par(p, old, new):
    """Replace text even when it is split over several runs. Returns True if replaced."""
    full = "".join(r.text for r in p.runs)
    i = full.find(old)
    if i < 0:
        return False
    pos = 0; first = True
    for r in p.runs:
        s, e = pos, pos + len(r.text); pos = e
        if e <= i or s >= i + len(old):
            continue
        a = max(i, s) - s; b = min(i + len(old), e) - s
        r.text = r.text[:a] + (new if first else "") + r.text[b:]
        first = False
    return True


def sub(d, old, new, expect=1):
    n = 0
    for p in all_paragraphs(d):
        while replace_in_par(p, old, new):
            n += 1
            if old in new: break
    if n < expect:
        sys.exit(f"!! '{old}' replaced {n}x, expected >= {expect}")


def set_par_text(p, text):
    for r in p.runs[1:]:
        r._r.getparent().remove(r._r)
    p.runs[0].text = text


def drop(p):
    p._p.getparent().remove(p._p)


def cell_single(cell, text):
    """Keep only first paragraph of a cell and put `text` into it."""
    for extra in cell.paragraphs[1:]:
        drop(extra)
    p = cell.paragraphs[0]
    if p.runs:
        set_par_text(p, text)
    else:
        p.add_run(text)


def unique_cells(row):
    seen = []; out = []
    for c in row.cells:
        if c._tc in seen: continue
        seen.append(c._tc); out.append(c)
    return out


def compact_row(row, height=340):
    """Rows generated for totals / discount / GST should not inherit the tall sample row height."""
    trPr = row._tr.get_or_add_trPr()
    for h in trPr.findall(qn("w:trHeight")):
        h.set(qn("w:val"), str(height))


def set_widths(table, widths):
    for g, w in zip(table._tbl.tblGrid.findall(qn("w:gridCol")), widths):
        g.set(qn("w:w"), str(w))
    for row in table.rows:
        for c, w in zip(unique_cells(row), widths):
            tcW = c._tc.tcPr.find(qn("w:tcW"))
            if tcW is not None:
                tcW.set(qn("w:w"), str(w))


def plain_runs(cell):
    """Amount cell of the sample is italic + underlined; make it a plain right aligned figure."""
    for r in cell._tc.iter(qn("w:rPr")):
        for tag in ("w:i", "w:u"):
            for e in r.findall(qn(tag)):
                r.remove(e)


def header_ref(d, ref_prefix_text):
    p = d.paragraphs[0]
    set_par_text(p, "Ref No: {{refNo}}" + " " * 100 + "{{proposalDate}}")


def build_software(tmp):
    d = docx.Document(to_docx(SAMPLES / "UD_Trucks-software-proposal-sample.doc", tmp))
    header_ref(d, None)
    ps = d.paragraphs
    set_par_text(ps[2], "{{customerName}}")
    set_par_text(ps[3], "{{customerAddress}}")
    for i in (6, 5, 4): drop(ps[i])
    set_par_text(d.paragraphs[8 - 3], d.paragraphs[8 - 3].text)  # no-op keeps title
    sub(d, "Hi Vijay", "Hi {{contactName}}")
    sub(d, "UD Trucks being", "{{customerName}} being")
    sub(d, "This commercial is for ICAI Chennai", "This commercial is for {{customerName}}")
    sub(d, "Coverage for about 7,000 assets", "Coverage for about {{assetCount}} assets")
    sub(d, "ICAI", "{{customerName}}", 2)
    sub(d, "Four Point", "{{customerName}}")
    sub(d, "(18%)", "({{gstPercent}}%)")
    sub(d, "Taxes: GST 18%", "Taxes: GST {{gstPercent}}%")
    sub(d, "valid for a period of 10 days", "valid for a period of {{validityDays}} days")
    sub(d, "100% advance", "{{advancePercentSoftware}}% advance")
    t = d.tables[0]
    proto = t.rows[1]
    c = unique_cells(proto)
    cell_single(c[0], "{{costRows.details}}")
    cell_single(c[1], "{{costRows.scope}}")
    cell_single(c[2], "{{costRows.amount}}")
    t._tbl.remove(t.rows[2]._tr)
    compact_row(proto)
    d.save(OUT / "software-proposal.docx")


def build_services(tmp):
    d = docx.Document(to_docx(SAMPLES / "Apexon-services-proposal-sample.doc", tmp))
    set_par_text(d.paragraphs[0], "Ref No: {{refNo}}" + " " * 100 + "{{proposalDate}}")
    ps = d.paragraphs
    set_par_text(ps[2], "{{customerName}}")
    set_par_text(ps[3], "{{customerAddress}}")
    for i in (7, 6, 5, 4): drop(ps[i])
    sub(d, "Hi Prasanna Srinivasa", "Hi {{contactName}}")
    sub(d, "Prabal Motors", "{{customerName}}")
    sub(d, "Approx 10,000 assets", "Approx {{assetCount}} assets")
    sub(d, "Venugopal Kulkarni", "{{contactName}}")
    sub(d, "Apexon", "{{customerName}}", 3)
    sub(d, "1 Collage locations", "{{branchCount}} locations")
    sub(d, "(18%)", "({{gstPercent}}%)")
    sub(d, "valid for a period of 10 days", "valid for a period of {{validityDays}} days")
    sub(d, "within 10 days", "within {{commencementDays}} days")
    sub(d, "Advance 50%", "Advance {{pay1}}%")
    sub(d, "40% Payment", "{{pay2}}% Payment")
    sub(d, "10% After", "{{pay3}}% After")
    sub(d, "Data Audit Team -1", "Data Audit Team -{{dataTeamSize}}")
    sub(d, "Barcoding)-4", "Barcoding)-{{trackingTeamSize}}")

    # Audit Locations vs Assets
    t = d.tables[0]
    c = unique_cells(t.rows[1])
    cell_single(c[1], "{{branchCount}}"); cell_single(c[2], "{{assetCount}}")

    # Projected project cost
    t = d.tables[2]
    c = unique_cells(t.rows[1])
    cell_single(c[0], "{{costRows.label}}")
    cell_single(c[1], "{{costRows.details}}")
    cell_single(c[2], "{{costRows.scope}}")
    cell_single(c[3], "{{costRows.amount}}")
    plain_runs(c[3])
    compact_row(t.rows[1])
    set_widths(t, [709, 4500, 3000, 1700])

    # Timeline
    t = d.tables[4]
    c = unique_cells(t.rows[1])
    cell_single(c[0], "{{phases.no}}"); cell_single(c[1], "{{phases.activity}}")
    cell_single(c[2], "{{phases.assets}}"); cell_single(c[3], "{{phases.days}}")
    t._tbl.remove(t.rows[3]._tr); t._tbl.remove(t.rows[2]._tr)
    rows = t.rows
    c = unique_cells(rows[2]); cell_single(c[0], "{{nextPhase}}")
    c = unique_cells(rows[4]); cell_single(c[1], "{{?hasSoftware}}Smart Assets Software Migration")
    d.save(OUT / "services-proposal.docx")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = tempfile.mkdtemp()
    try:
        build_software(tmp); build_services(tmp)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print("templates written to", OUT)
