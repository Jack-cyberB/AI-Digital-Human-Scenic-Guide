"""
CosyVoice 2 TTS microservice for JingQu digital human.
Requires: pip install cosyvoice fastapi uvicorn
Start: python cosyvoice_server.py --port 50021
"""
from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
import uvicorn
import io
import argparse
import soundfile as sf

app = FastAPI(title="CosyVoice2-TTS")

# Lazy-load model
_tts = None

def get_tts():
    global _tts
    if _tts is None:
        from cosyvoice.cli.cosyvoice import CosyVoice2
        _tts = CosyVoice2('pretrained_models/CosyVoice2-0.5B', load_jit=False, load_trt=False, fp16=False)
    return _tts

@app.post("/tts")
async def tts(text: str = "", voice: str = "default"):
    """Convert text to speech, returns WAV audio"""
    if not text.strip():
        raise HTTPException(400, "text is empty")
    try:
        tts_engine = get_tts()
        output = io.BytesIO()
        for i, result in enumerate(tts_engine.inference_sft(text, voice, stream=False)):
            sf.write(output, result['tts_speech'].numpy(), 22050, format='WAV')
            break  # single utterance
        return Response(content=output.getvalue(), media_type="audio/wav")
    except Exception as e:
        raise HTTPException(500, str(e))

@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": _tts is not None}

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=50021)
    parser.add_argument("--host", default="127.0.0.1")
    args = parser.parse_args()
    uvicorn.run(app, host=args.host, port=args.port)
