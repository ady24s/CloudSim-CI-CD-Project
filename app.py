import re
from fastapi import FastAPI
from fastapi.responses import FileResponse, JSONResponse
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import subprocess
import os

app = FastAPI()

# CORS setup
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount frontend folder
app.mount("/frontend", StaticFiles(directory="frontend"), name="frontend")

@app.get("/")
def serve_index():
    return FileResponse(os.path.join("frontend", "index.html"))

# Util to remove ANSI terminal color codes
def strip_ansi(text):
    ansi_escape = re.compile(r'\x1B(?:[@-Z\\-_]|\[[0-?]*[ -/]*[@-~])')
    return ansi_escape.sub('', text)


@app.get("/simulate")
def simulate(): 
    result = subprocess.run(
        ["java", "-cp", "target/cloudsim-result-sim-1.0-SNAPSHOT-jar-with-dependencies.jar", "com.result.sim.ResultTrafficSimulation"],
        capture_output=True, text=True
    )
    clean_output = strip_ansi(result.stdout)
    return JSONResponse(content={"output": clean_output})
