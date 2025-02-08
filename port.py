import socket
import time
import threading

stop_event = threading.Event()
threads = []

def scan_ports(ip, port_range, delay):
    stop_event.clear()
    global threads
    threads = []

    try:
        if "-" in port_range:
            start, end = map(int, port_range.split("-"))
        else:
            start, end = int(port_range), int(port_range)

        results = []
        lock = threading.Lock()

        def scan_port(port):
            if stop_event.is_set():
                return

            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(1)
            try:
                result = sock.connect_ex((ip, port))
                if result == 0:
                    with lock:
                        results.append(f"Port {port} is open")
            finally:
                sock.close()

            time.sleep(delay / 1000.0)

        for port in range(start, end + 1):
            if stop_event.is_set():
                break
            thread = threading.Thread(target=scan_port, args=(port,))
            threads.append(thread)
            thread.start()

        for thread in threads:
            thread.join()

        return "\n".join(results)

    except Exception as e:
        return str(e)

def stop_scan():
    stop_event.set()
    for thread in threads:
        if thread.is_alive():
            thread.join(timeout=0.5)
