import json
import os
import sys
import subprocess

# Try to import psutil, install if missing
try:
    import psutil
except ImportError:
    try:
        # Install psutil automatically
        subprocess.check_call([sys.executable, "-m", "pip", "install", "psutil", "--break-system-packages"])
        import psutil
    except Exception as e:
        print(json.dumps({"error": f"Failed to install psutil: {str(e)}"}, indent=2))
        sys.exit(1)

def get_size(bytes, suffix="B"):
    """
    Scale bytes to its proper format
    e.g:
        1253656 => '1.20MB'
        1253656678 => '1.17GB'
    """
    factor = 1024
    for unit in ["", "K", "M", "G", "T", "P"]:
        if bytes < factor:
            return f"{bytes:.2f}{unit}{suffix}"
        bytes /= factor

def get_system_status():
    try:
        # CPU Usage
        cpu_usage = psutil.cpu_percent(interval=1)
        
        # Memory Usage
        svmem = psutil.virtual_memory()
        memory_info = {
            "total": get_size(svmem.total),
            "available": get_size(svmem.available),
            "used": get_size(svmem.used),
            "percent": f"{svmem.percent}%"
        }
        
        # Disk Usage
        disk_usage = psutil.disk_usage('/')
        disk_info = {
            "total": get_size(disk_usage.total),
            "used": get_size(disk_usage.used),
            "free": get_size(disk_usage.free),
            "percent": f"{disk_usage.percent}%"
        }
        
        # Process Count (Non-system processes roughly estimated by user)
        # Note: Counting "non-system" processes is tricky cross-platform.
        # Here we count processes owned by the current user as a proxy for "user processes"
        # or simply return total count if we want to be generic. 
        # The prompt asked for "non-system processes", which usually implies user-space apps.
        
        current_user = os.getlogin()
        process_count = 0
        user_process_count = 0
        
        for proc in psutil.process_iter(['username']):
            try:
                process_count += 1
                if proc.info['username'] == current_user:
                    user_process_count += 1
            except (psutil.NoSuchProcess, psutil.AccessDenied, psutil.ZombieProcess):
                pass
                
        status = {
            "cpu_usage": f"{cpu_usage}%",
            "memory": memory_info,
            "disk": disk_info,
            "total_processes": process_count,
            "user_processes": user_process_count
        }
        
        return status
        
    except Exception as e:
        return {"error": str(e)}

if __name__ == "__main__":
    # Ensure psutil is installed: pip install psutil
    try:
        status = get_system_status()
        print(json.dumps(status, indent=2))
    except Exception as e:
        print(json.dumps({"error": f"An unexpected error occurred: {str(e)}"}, indent=2))
        sys.exit(1)
