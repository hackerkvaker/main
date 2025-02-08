import sqlite3
import os
from datetime import datetime
import geoip2.database
import requests
import whois

asn_db_path = os.path.join(os.path.dirname(__file__), "1.mmdb")
country_db_path = os.path.join(os.path.dirname(__file__), "2.mmdb")
city_db_path = os.path.join(os.path.dirname(__file__), "3.mmdb")
db_path = os.path.join(os.path.dirname(__file__), "4.db")

def save_to_db(identifier, result, is_ip):
    try:
        with sqlite3.connect(db_path) as conn:
            cursor = conn.cursor()
            if is_ip:
                cursor.execute("""
                INSERT OR IGNORE INTO ip (ip_address, asn, organization, country, city, region, timezone, timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, (identifier, result['asn'], result['organization'], result['country'], result['city'],
                      result['region'], result['timezone'], datetime.now().isoformat()))
            else:
                cursor.execute("""
                INSERT OR IGNORE INTO website (url, status_code, content_type, whois_info, timestamp)
                VALUES (?, ?, ?, ?, ?)
                """, (identifier, result['status_code'], result['content_type'], result['whois_info'], datetime.now().isoformat()))
            conn.commit()
            print(f"Data for {identifier} saved successfully.")
    except Exception as e:
        print(f"Error saving to database: {e}")

def scan_ip(ip):
    try:
        with geoip2.database.Reader(asn_db_path) as asn_reader:
            asn_response = asn_reader.asn(ip)
            asn_info = asn_response.autonomous_system_number
            organization = asn_response.autonomous_system_organization
        with geoip2.database.Reader(country_db_path) as country_reader:
            country_response = country_reader.country(ip)
            country_info = country_response.country.name
        with geoip2.database.Reader(city_db_path) as city_reader:
            city_response = city_reader.city(ip)
            city_info = city_response.city.name
            region_info = city_response.subdivisions.most_specific.name
            timezone_info = city_response.location.time_zone

        return {
            'asn': asn_info,
            'organization': organization,
            'country': country_info,
            'city': city_info,
            'region': region_info,
            'timezone': timezone_info
        }

    except geoip2.errors.AddressNotFoundError:
        return f"Error: IP {ip} not found in database"
    except Exception as e:
        return f"Error: {str(e)}"

def scan_website(url):
    try:
        response = requests.get(f"http://{url}", timeout=5)
        website_info = {
            'status_code': response.status_code,
            'content_type': response.headers.get('Content-Type'),
            'whois_info': get_full_whois_info(url)
        }
        return website_info
    except requests.exceptions.RequestException as e:
        return f"Error: Could not reach website {url}. Details: {str(e)}"

def get_full_whois_info(domain):
    try:
        w = whois.whois(domain)
        whois_info = "\n".join([f"{key}: {value}" for key, value in w.items() if value])
        return whois_info if whois_info else "No WHOIS information available."
    except Exception as e:
        return f"Error: Unable to retrieve WHOIS information for {domain}. Details: {str(e)}"

def scan_site(ip_or_url):
    try:
        if ip_or_url.count('.') == 3 and all(0 <= int(part) < 256 for part in ip_or_url.split('.')):
            result = scan_ip(ip_or_url)
            save_to_db(ip_or_url, result, is_ip=True)
        else:
            result = scan_website(ip_or_url)
            save_to_db(ip_or_url, result, is_ip=False)

        return result
    except Exception as e:
        return f"Error: {str(e)}"
