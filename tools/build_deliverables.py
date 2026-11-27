"""Generate the report, UML figures, test summary and source ZIP.

Run Maven verify first. This script never runs builds or contacts the network.
V2 report: biases, layer decorators and tested AspectJ application.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import hashlib
from html import escape
import json
import math
from pathlib import Path
import re
import textwrap
import xml.etree.ElementTree as ET
from zipfile import ZIP_DEFLATED, ZipFile

from PIL import Image as PILImage, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image, PageBreak, Paragraph, Preformatted, SimpleDocTemplate, Spacer,
)

ROOT = Path(__file__).resolve().parents[1]
NEURAL = (ROOT / "network-core").is_dir()
INK = "#17324d"
ACCENT = "#087e8b"


def font_paths():
    candidates = [
        Path("C:/Windows/Fonts/arial.ttf"),
        Path("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"),
        Path("/Library/Fonts/Arial.ttf"),
    ]
    regular = next((p for p in candidates if p.exists()), None)
    if regular is None:
        raise RuntimeError("Install Arial or DejaVu Sans to render accented text.")
    bold = regular.with_name(
        "arialbd.ttf" if regular.name.lower() == "arial.ttf" else "DejaVuSans-Bold.ttf"
    )
    return regular, bold if bold.exists() else regular


REGULAR, BOLD = font_paths()


class Diagram:
    """Small explicit UML renderer. Coordinates and edges are versioned below."""

    def __init__(self, width, height, title):
        self.image = PILImage.new("RGB", (width, height), "white")
        self.draw = ImageDraw.Draw(self.image)
        self.font = ImageFont.truetype(str(REGULAR), 25)
        self.bold = ImageFont.truetype(str(BOLD), 28)
        self.small = ImageFont.truetype(str(REGULAR), 23)
        self.draw.text((30, 12), title, font=self.bold, fill=INK)

    def box(self, rect, title, lines=(), interface=False):
        x1, y1, x2, y2 = rect
        self.draw.rectangle(rect, fill="#f1f7fa", outline=INK, width=3)
        header = ("«interface» " if interface else "") + title
        size = 28
        while self.draw.textlength(header, font=ImageFont.truetype(str(BOLD), size)) > x2-x1-18:
            size -= 1
        self.draw.text((x1+12, y1+12), header,
                       font=ImageFont.truetype(str(BOLD), size), fill=INK)
        self.draw.line((x1, y1+55, x2, y1+55), fill=INK, width=2)
        for i, line in enumerate(lines):
            font = self.font
            if self.draw.textlength(line, font=font) > x2-x1-20:
                font = self.small
            if self.draw.textlength(line, font=font) > x2-x1-20:
                raise ValueError(f"Diagram text too wide: {line}")
            self.draw.text((x1+12, y1+68+i*34), line, font=font, fill=INK)

    def edge(self, points, label="", at=None, kind="arrow", dashed=False):
        for a, b in zip(points, points[1:]):
            if not dashed:
                self.draw.line((*a, *b), fill=ACCENT, width=3)
            else:
                length = math.dist(a, b)
                for step in range(0, int(length), 17):
                    end = min(step+10, length)
                    p = (a[0]+(b[0]-a[0])*step/length, a[1]+(b[1]-a[1])*step/length)
                    q = (a[0]+(b[0]-a[0])*end/length, a[1]+(b[1]-a[1])*end/length)
                    self.draw.line((*p, *q), fill=ACCENT, width=3)
        tip, prev = points[-1], points[-2]
        angle = math.atan2(tip[1]-prev[1], tip[0]-prev[0])
        back = (tip[0]-22*math.cos(angle), tip[1]-22*math.sin(angle))
        left = (back[0]+10*math.sin(angle), back[1]-10*math.cos(angle))
        right = (back[0]-10*math.sin(angle), back[1]+10*math.cos(angle))
        if kind == "realize":
            self.draw.polygon([tip, left, right], fill="white", outline=ACCENT, width=3)
        elif kind != "composition":
            self.draw.line([left, tip, right], fill=ACCENT, width=3)
        if kind == "composition":
            start, following = points[0], points[1]
            angle = math.atan2(following[1]-start[1], following[0]-start[0])
            middle = (start[0]+13*math.cos(angle), start[1]+13*math.sin(angle))
            end = (start[0]+26*math.cos(angle), start[1]+26*math.sin(angle))
            self.draw.polygon([
                start, (middle[0]+8*math.sin(angle), middle[1]-8*math.cos(angle)),
                end, (middle[0]-8*math.sin(angle), middle[1]+8*math.cos(angle)),
            ], fill=INK)
        if label:
            x, y = at
            bounds = self.draw.textbbox((x, y), label, font=self.small)
            self.draw.rectangle((bounds[0]-4, bounds[1]-3, bounds[2]+4, bounds[3]+3), fill="white")
            self.draw.text((x, y), label, font=self.small, fill=INK)

    def save(self, name):
        self.image.save(ROOT / "docs" / name)


def diagrams():
    if not NEURAL:
        return
    d = Diagram(1500, 1600, "UML V2 — Modèle, activations et construction")
    d.box((540, 90, 960, 295), "NeuralNetwork",
          ["- layers / connections", "+ train(...) / fit(...)", "+ predict(...) : double[]", "+ setActivation(...)"])
    d.box((40, 90, 440, 225), "NetworkBuilder", ["+ input / hidden / output", "+ build() : NeuralNetwork"])
    d.box((1080, 90, 1480, 225), "ActivationFactory", ["+ create(Type)", "Simple Factory"])
    d.box((40, 400, 440, 535), "LayerComponent",
          ["+ type() / neurons()", "+ activate / derivative"], interface=True)
    d.box((1040, 400, 1480, 570), "Connection",
          ["- weight : double [0,1]", "- source : Neuron", "- destination : Neuron"])
    d.box((40, 695, 440, 900), "Neuron",
          ["- index : int", "- incoming / outgoing", "- activation", "- bias : double"])
    d.box((580, 735, 1080, 905), "ActivationFunction",
          ["+ activate(sums, index)", "+ derivative(sums, i, j)", "Contrat vectoriel"], interface=True)
    d.edge([(440, 155), (540, 155)], "construit", (445, 112), dashed=True)
    d.edge([(1080, 155), (1010, 155), (1010, 340), (830, 340), (830, 735)],
           "crée", (845, 345), dashed=True)
    d.edge([(540, 245), (490, 245), (490, 455), (440, 455)],
           "1 / 2..* couches", (235, 325), kind="composition")
    d.edge([(960, 245), (1000, 245), (1000, 460), (1040, 460)],
           "1 / 0..* connexions", (1060, 325), kind="composition")
    d.edge([(240, 535), (240, 695)], "neurones : 1..*", (255, 600))
    d.edge([(440, 805), (580, 805)], "1", (495, 770))
    d.edge([(1260, 570), (1260, 635), (465, 635), (465, 740), (440, 740)],
           "source et destination : 1 chacune", (560, 595))
    for rect, name, lines in [
        ((30, 1090, 320, 1230), "Identity", ["y = x", "dérivée diagonale"]),
        ((360, 1090, 660, 1230), "Sigmoid", ["y = 1/(1+exp(-x))", "dérivée y*(1-y)"]),
        ((700, 1090, 1000, 1230), "Softmax", ["normalisation couche", "Jacobienne complète"]),
        ((1040, 1090, 1480, 1230), "LegacyActivationAdapter", ["- ancienne", "délègue le calcul"]),
    ]:
        d.box(rect, name, lines)
        center = (rect[0]+rect[2])//2
        d.edge([(center, 1090), (center, 995), (830, 995), (830, 905)],
               kind="realize", dashed=True)
    d.box((1040, 1310, 1480, 1430), "LegacyActivation",
          ["+ calculer / derivee"], interface=True)
    d.edge([(1260, 1230), (1260, 1310)], "adapte", (1280, 1245))
    d.box((1040, 1490, 1480, 1590), "AncienneFonctionTanh", ["implémentation historique"])
    d.edge([(1260, 1490), (1260, 1430)], kind="realize", dashed=True)
    d.draw.text((30, 1360), "Losange plein : composition. Triangle pointillé : réalisation.", font=d.small, fill=INK)
    d.draw.text((30, 1400), "Layer concrète possède les neurones (voir Decorator).", font=d.small, fill=INK)
    d.draw.text((30, 1440), "Connexions entre couches consécutives ; biais finis, poids [0,1].", font=d.small, fill=INK)
    d.save("classes-modele.png")

    d = Diagram(1500, 1120, "UML V2 — Decorator : enrichissement dynamique de chaque couche")
    d.box((400, 80, 1100, 280), "LayerComponent",
          ["+ type() : LayerType", "+ neurons() : List<Neuron>",
           "+ activate(sums) / derivative(sums,i,j)"], interface=True)
    d.box((40, 440, 650, 640), "Layer",
          ["- type : LayerType", "- neurons : List<Neuron>", "calcul des activations de base"])
    d.box((850, 440, 1460, 640), "LayerDecorator",
          ["- component : LayerComponent", "+ component()", "délégation (classe abstraite)"])
    d.edge([(345, 440), (345, 340), (750, 340), (750, 280)], kind="realize", dashed=True)
    d.edge([(1155, 440), (1155, 340), (750, 340), (750, 280)], kind="realize", dashed=True)
    d.edge([(1460, 520), (1480, 520), (1480, 175), (1100, 175)],
           "enveloppe : 1", (1210, 120))
    d.box((40, 850, 680, 1050), "StatisticsLayerDecorator",
          ["- statistics : Statistics", "+ activate() : compte / min / max / moyenne",
           "gradient inchangé"])
    d.box((830, 850, 1460, 1050), "ScalingLayerDecorator",
          ["- factor : double", "+ activate() : factor * sortie",
           "+ derivative() : factor * dérivée"])
    d.edge([(360, 850), (360, 745), (1155, 745), (1155, 640)], kind="realize")
    d.edge([(1145, 850), (1145, 745), (1155, 745), (1155, 640)], kind="realize")
    d.draw.text((40, 1080), "Triangle : héritage ; pointillés : réalisation d'interface. Empilage sans copier les neurones.",
                font=d.small, fill=INK)
    d.save("classes-decorator.png")

    d = Diagram(1500, 800, "UML — State et Observer")
    d.box((40, 80, 470, 245), "NeuralNetwork",
          ["- state : NetworkState", "- observers : Set", "+ subscribe / unsubscribe"])
    d.box((780, 80, 1450, 280), "NetworkState",
          ["+ name()", "+ requireConfiguration()", "+ requireTraining() / requirePrediction()"], interface=True)
    d.edge([(470, 160), (780, 160)], "état courant : 1", (500, 120))
    for x, name in [(40, "ConstructionState"), (545, "TrainingState"), (1050, "TrainedState")]:
        d.box((x, 390, x+410, 485), name, ["politique d'autorisation"])
        d.edge([(x+205, 390), (x+205, 330), (1100, 330), (1100, 280)],
               kind="realize", dashed=True)
    d.box((40, 625, 650, 755), "NetworkObserver",
          ["+ onStateChange(StateChange)"], interface=True)
    d.box((930, 605, 1460, 775), "Dashboard",
          ["+ events() : List<StateChange>", "+ statistics() : Map<Name, Long>", "historique et compteurs"])
    d.edge([(100, 245), (20, 245), (20, 580), (330, 580), (330, 625)],
           "notifie 0..* observateurs", (70, 545))
    d.edge([(930, 685), (650, 685)], kind="realize", dashed=True)
    d.save("classes-etats.png")

    d = Diagram(1500, 580, "Cycle de vie — transitions notifiées")
    d.box((40, 200, 430, 320), "Construction", ["configuration permise"])
    d.box((555, 200, 945, 320), "Training", ["opérations réentrantes refusées"])
    d.box((1070, 200, 1460, 320), "Trained", ["predict autorisé"])
    d.edge([(430, 235), (555, 235)], "train / fit", (435, 175))
    d.edge([(945, 235), (1070, 235)], "succès", (970, 175))
    d.edge([(1210, 200), (1210, 90), (750, 90), (750, 200)],
           "nouveau train / fit", (810, 50))
    d.edge([(670, 320), (670, 410), (240, 410), (240, 320)],
           "échec : retour à Construction", (245, 420))
    d.edge([(840, 320), (840, 410), (1240, 410), (1240, 320)],
           "échec : retour à Trained", (915, 420))
    d.edge([(1410, 320), (1410, 525), (90, 525), (90, 320)],
           "reconfiguration après entraînement : invalidation", (380, 530))
    d.save("etats.png")


def export_tests(allow_missing=False):
    suites = []
    for file in sorted(ROOT.rglob("TEST-*.xml")):
        if "surefire-reports" not in file.parts:
            continue
        suite = ET.parse(file).getroot()
        suites.append({
            "suite": suite.attrib["name"],
            "source": file.relative_to(ROOT).as_posix(),
            **{key: int(suite.attrib.get(key, 0))
               for key in ("tests", "failures", "errors", "skipped")},
            "cases": [case.attrib["name"] for case in suite.findall("testcase")],
        })
    expected = 53 if NEURAL else 15
    totals = {key: sum(s[key] for s in suites)
              for key in ("tests", "failures", "errors", "skipped")}
    if not allow_missing and (totals["tests"] != expected or any(
            totals[key] for key in ("failures", "errors", "skipped"))):
        raise RuntimeError(f"Run all Maven verify commands first. Expected {expected} passing tests: {totals}")
    result = {
        "generated_at_utc": datetime.now(timezone.utc).isoformat(),
        "note": "Local Surefire results, not a code coverage measurement.",
        "totals": totals,
        "suites": suites,
    }
    (ROOT / "docs" / "resultats-tests.json").write_text(
        json.dumps(result, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    return totals


def inline(text):
    text = escape(text)
    text = re.sub(r"`([^`]+)`", r'<font name="Code">\1</font>', text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)
    return text


def report():
    pdfmetrics.registerFont(TTFont("Body", str(REGULAR)))
    pdfmetrics.registerFont(TTFont("Body-Bold", str(BOLD)))
    pdfmetrics.registerFontFamily("Body", normal="Body", bold="Body-Bold",
                                  italic="Body", boldItalic="Body-Bold")
    # Use a Unicode font for code too, so copied accents remain readable.
    pdfmetrics.registerFont(TTFont("Code", str(REGULAR)))
    styles = {
        "body": ParagraphStyle("body", fontName="Body", fontSize=9.5, leading=14.3,
                               spaceAfter=9, textColor=colors.HexColor(INK)),
        "title": ParagraphStyle("title", fontName="Body-Bold", fontSize=25, leading=31,
                                spaceAfter=24, textColor=colors.HexColor(INK)),
        "h2": ParagraphStyle("h2", fontName="Body-Bold", fontSize=17, leading=22,
                             spaceBefore=10, spaceAfter=12, keepWithNext=True,
                             textColor=colors.HexColor(ACCENT)),
        "h3": ParagraphStyle("h3", fontName="Body-Bold", fontSize=11.5, leading=16,
                             spaceBefore=9, spaceAfter=7, keepWithNext=True,
                             textColor=colors.HexColor(INK)),
        "code": ParagraphStyle("code", fontName="Code", fontSize=8, leading=12,
                               spaceAfter=12, backColor=colors.HexColor("#eef4f7"),
                               borderPadding=8),
        "caption": ParagraphStyle("caption", fontName="Body", fontSize=8, leading=11,
                                  alignment=TA_CENTER, spaceAfter=12),
    }
    story, paragraph, code = [], [], None

    def flush():
        if paragraph:
            story.append(Paragraph(inline(" ".join(paragraph)), styles["body"]))
            paragraph.clear()

    for line in (ROOT / "RAPPORT.md").read_text(encoding="utf-8").splitlines():
        if line.startswith("```"):
            flush()
            if code is None:
                code = []
            else:
                wrapped = []
                for source_line in code:
                    wrapped.extend(textwrap.wrap(source_line, width=94,
                        replace_whitespace=False, drop_whitespace=False) or [""])
                story.append(Preformatted("\n".join(wrapped), styles["code"]))
                code = None
            continue
        if code is not None:
            code.append(line)
            continue
        if line == "<!-- pagebreak -->":
            flush()
            story.append(PageBreak())
        elif not line.strip():
            flush()
        elif line.startswith("#"):
            flush()
            level = len(line)-len(line.lstrip("#"))
            style = "title" if level == 1 else ("h2" if level == 2 else "h3")
            story.append(Paragraph(inline(line[level:].strip()), styles[style]))
        elif line.startswith("!["):
            flush()
            match = re.fullmatch(r"!\[(.*)\]\((.*)\)", line)
            if not match:
                raise ValueError(f"Invalid Markdown image: {line}")
            caption, path = match.groups()
            picture = Image(str(ROOT / path))
            ratio = min(1, 487/picture.imageWidth, 475/picture.imageHeight)
            picture.drawWidth = picture.imageWidth*ratio
            picture.drawHeight = picture.imageHeight*ratio
            story.extend([picture, Spacer(1, 8), Paragraph(escape(caption), styles["caption"])])
        elif line.startswith("- "):
            flush()
            story.append(Paragraph("• "+inline(line[2:]), styles["body"]))
        else:
            paragraph.append(line)
    flush()
    if code is not None:
        raise ValueError("Unclosed code block")

    def footer(canvas, doc):
        canvas.saveState()
        width, height = A4
        canvas.setStrokeColor(colors.HexColor(ACCENT))
        canvas.line(54, height-32, width-54, height-32)
        canvas.setFont("Body", 8)
        canvas.setFillColor(colors.HexColor(INK))
        label = "Examen V2 • Réseau de neurones" if NEURAL else "Activité pratique • AspectJ et Spring AOP"
        canvas.drawString(54, 29, label)
        canvas.drawRightString(width-54, 29, str(doc.page))
        canvas.restoreState()

    pdf = ROOT / "dist" / "RAPPORT.pdf"
    doc = SimpleDocTemplate(str(pdf), pagesize=A4, leftMargin=54, rightMargin=54,
        topMargin=48, bottomMargin=48, title=(ROOT / "RAPPORT.md").read_text(
            encoding="utf-8").splitlines()[0][2:], author="Rapport pédagogique",
        pageCompression=1)
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    return pdf


def archive():
    excluded = {".git", "target", "logs", ".idea", ".vscode", "__pycache__", ".venv"}
    files = []
    for path in sorted(ROOT.rglob("*")):
        if not path.is_file():
            continue
        relative = path.relative_to(ROOT)
        if any(part in excluded for part in relative.parts):
            continue
        if path.suffix.lower() in {".zip", ".pyc", ".iml"} or ".log" in path.name.lower():
            continue
        files.append(path)
    archive_path = ROOT / "dist" / f"{ROOT.name}.zip"
    with ZipFile(archive_path, "w", ZIP_DEFLATED) as output:
        manifest = []
        for path in files:
            relative = path.relative_to(ROOT).as_posix()
            output.write(path, f"{ROOT.name}/{relative}")
            manifest.append(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {relative}")
        output.writestr(f"{ROOT.name}/SHA256SUMS.txt", "\n".join(manifest)+"\n")
    with ZipFile(archive_path) as output:
        if output.testzip() is not None:
            raise RuntimeError("ZIP integrity validation failed")
    return archive_path


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-missing-tests", action="store_true",
        help="Generate without a complete passing Surefire run (not for submission).")
    args = parser.parse_args()
    (ROOT / "docs").mkdir(exist_ok=True)
    (ROOT / "dist").mkdir(exist_ok=True)
    totals = export_tests(args.allow_missing_tests)
    diagrams()
    pdf = report()
    zipped = archive()
    print(json.dumps({"pdf": str(pdf), "zip": str(zipped), "tests": totals},
                     ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
