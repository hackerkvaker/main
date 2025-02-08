import sqlite3
import os
import json

def get_db_connection():
    db_path = os.path.join(os.path.dirname(__file__), "4.db")
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    return conn

def get_all_results():
    try:
        conn = get_db_connection()
        cursor = conn.cursor()

        cursor.execute("SELECT * FROM website")
        website_rows = cursor.fetchall()

        cursor.execute("SELECT * FROM ip")
        ip_rows = cursor.fetchall()

        result = {
            "website": [{"id": row["id"], "url": row["url"], "status_code": row["status_code"],
                         "content_type": row["content_type"], "whois_info": row["whois_info"],
                         "timestamp": row["timestamp"]} for row in website_rows],
            "ip": [{"id": row["id"], "ip_address": row["ip_address"], "asn": row["asn"],
                    "organization": row["organization"], "country": row["country"],
                    "city": row["city"], "region": row["region"], "timezone": row["timezone"],
                    "timestamp": row["timestamp"]} for row in ip_rows]
        }

        conn.close()
        return json.dumps(result)

    except Exception as e:
        return json.dumps({"log_response": f"Error: {str(e)}"})
