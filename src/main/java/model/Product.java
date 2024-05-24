package model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
    @JsonProperty("Vendor File Name 1")
    public String vendorFileName1;
    @JsonProperty("DMM/GRP ID")
    public String dmmGrpId;
    @JsonProperty("Color")
    public String color;
    @JsonProperty("Class Name")
    public String className;
    @JsonProperty("Vendor Style/VPN")
    public String vendorStyleVpn;
    @JsonProperty("Brand Name")
    public String brandName;
    @JsonProperty("SVS")
    public String svs;
    @JsonProperty("Image Upload Date in Workhorse")
    public String imageUploadDate;
    @JsonProperty("Photo Override UPC")
    public String photoOverrideUpc;
    @JsonProperty("Ecom OH Units")
    public String ecomOhUnits;
    @JsonProperty("Ecom OO Units")
    public String ecomOoUnits;
    @JsonProperty("GMM/DIV ID")
    public String gmmDivId;
    @JsonProperty("Style Description")
    public String styleDescription;
    // Getters and setters
    public String getVendorFileName1() {
        return vendorFileName1;
    }
    public String getDmmGrpId() {
        return dmmGrpId;
    }
    public String getColor() {
        return color;
    }
    public String getClassName() {
        return className;
    }
    public String getVendorStyleVpn() {
        return vendorStyleVpn;
    }
    public String getBrandName() {
        return brandName;
    }
    public String getSvs() {
        return svs;
    }
    public String getImageUploadDate() {
        return imageUploadDate;
    }
    public String getPhotoOverrideUpc() {
        return photoOverrideUpc;
    }
    public String getEcomOhUnits() {
        return ecomOhUnits;
    }
    public String getEcomOoUnits() {
        return ecomOoUnits;
    }
    public String getGmmDivId() {
        return gmmDivId;
    }
    public String getStyleDescription() {
        return styleDescription;
    }
}
