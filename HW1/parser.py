import json
import requests
import gzip
import tempfile

# Obtain the JSON data
url = "https://nvd.nist.gov/feeds/json/cve/1.1/nvdcve-1.1-2023.json.gz"#https://nvd.nist.gov/feeds/json/cve/1.1/nvdcve-1.1-recent.json.gz"
response = requests.get(url)


# Save the JSON feed in a temporary file
with tempfile.TemporaryFile() as temp:
    temp.write(response.content) # save gzip contents to file
    temp.seek(0) # reset the file handler to the beginning of file
    with gzip.open(temp, 'rb') as f:
        file_content = f.read()

        data = json.loads(file_content)

        # Extract the relevant information
        for entry in data['CVE_Items']:
            cve_id = entry['cve']['CVE_data_meta']['ID']
            cve_description = entry['cve']['description']['description_data'][0]['value']
            if cve_id == "CVE-2023-24455":
                print("CVE ID: ", cve_id)
                print("Description: ", cve_description)
                
                for config in entry['configurations']['nodes']:
                    print(config)
                    for cpe in config["cpe_match"]:
                        print("CPE", cpe)
    
            
