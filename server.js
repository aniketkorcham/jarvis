import express from "express";
import OpenAI from "openai";

const app = express();
app.use(express.json({ limit: "16kb" }));

if (!process.env.OPENAI_API_KEY) {
  console.error("Missing OPENAI_API_KEY environment variable.");
  process.exit(1);
}
const openai = new OpenAI({ apiKey: process.env.OPENAI_API_KEY });

app.get("/health", (_req, res) => res.json({ status: "JARVIS backend online" }));

app.post("/chat", async (req, res) => {
  try {
    const message = String(req.body?.message ?? "").trim().slice(0, 4000);
    const language = String(req.body?.language ?? "English").slice(0, 40);
    if (!message) return res.status(400).json({ error: "Message is required." });

    const result = await openai.responses.create({
      model: "gpt-4.1-mini",
      instructions: [
        "You are JARVIS, a concise, polite Android personal assistant.",
        `Reply in ${language} when possible. Understand Hindi, English, and Marathi.`,
        "You cannot control the phone directly. Never claim you opened, sent, changed, or completed an action.",
        "For calls, payments, messages, passwords, OTPs, and financial actions, require user review and confirmation.",
        "Never request passwords, OTPs, banking PINs, or recovery codes."
      ].join(" "),
      input: message
    });
    res.json({ reply: result.output_text || "I could not prepare a response." });
  } catch (error) {
    console.error("OpenAI request failed:", error?.message || error);
    res.status(500).json({ error: "AI request failed." });
  }
});

const port = Number(process.env.PORT || 3000);
app.listen(port, "0.0.0.0", () => console.log(`JARVIS backend listening on ${port}`));
