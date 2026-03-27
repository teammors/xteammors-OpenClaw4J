#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
天气预报技能
获取指定城市的天气预报
"""

import sys
import json
import requests
from datetime import datetime

def get_city_coordinates(city):
    """获取城市的经纬度
    使用预定义的城市经纬度映射
    """
    # 城市经纬度映射
    city_coords = {
        "北京": {"latitude": 39.9042, "longitude": 116.4074},
        "上海": {"latitude": 31.2304, "longitude": 121.4737},
        "广州": {"latitude": 23.1291, "longitude": 113.2644},
        "深圳": {"latitude": 22.5431, "longitude": 114.0579},
        "厦门": {"latitude": 24.4798, "longitude": 118.0819},
        "福州": {"latitude": 26.0745, "longitude": 119.2965},
        "杭州": {"latitude": 30.2741, "longitude": 120.1551},
        "成都": {"latitude": 30.5728, "longitude": 104.0668},
        "武汉": {"latitude": 30.5928, "longitude": 114.3055},
        "西安": {"latitude": 34.3416, "longitude": 108.9398},
        "南京": {"latitude": 32.0603, "longitude": 118.7969},
        "重庆": {"latitude": 29.4316, "longitude": 106.9123},
        "天津": {"latitude": 39.0842, "longitude": 117.2009},
        "苏州": {"latitude": 31.2989, "longitude": 120.5853},
        "郑州": {"latitude": 34.7466, "longitude": 113.6253},
        "长沙": {"latitude": 28.2278, "longitude": 112.9388},
        "青岛": {"latitude": 36.0671, "longitude": 120.3826},
        "大连": {"latitude": 38.9140, "longitude": 121.6147},
        "宁波": {"latitude": 29.8683, "longitude": 121.5440}
    }
    
    if city in city_coords:
        return city_coords[city]
    else:
        # 如果没有找到城市，使用默认值（北京）
        return city_coords.get("北京")

def get_weather(input_str):
    """获取指定城市的天气预报"""
    try:
        # 解析输入字符串
        if "城市：" in input_str and "纬度：" in input_str and "经度：" in input_str:
            # 从AI返回的格式中提取城市、纬度和经度
            lines = input_str.strip().split('\n')
            city = ""
            latitude = 0.0
            longitude = 0.0
            
            for line in lines:
                if line.startswith("城市："):
                    city = line.split("城市：")[1].strip()
                elif line.startswith("纬度："):
                    latitude = float(line.split("纬度：")[1].strip())
                elif line.startswith("经度："):
                    longitude = float(line.split("经度：")[1].strip())
        else:
            # 直接使用输入作为城市名称
            city = input_str.strip()
            coords = get_city_coordinates(city)
            latitude = coords["latitude"]
            longitude = coords["longitude"]
        
        # 使用 Open-Meteo API 获取天气数据
        # 免费额度：永久免费，无需 API Key，支持全球经纬度查询
        base_url = "https://api.open-meteo.com/v1/forecast"
        params = {
            "latitude": latitude,
            "longitude": longitude,
            "current_weather": True,
            "hourly": "temperature_2m,relative_humidity_2m,wind_speed_10m",
            "daily": "temperature_2m_max,temperature_2m_min",
            "timezone": "Asia/Shanghai"
        }
        
        response = requests.get(base_url, params=params)
        response.raise_for_status()
        data = response.json()
        
        # 解析天气数据
        current_weather = data.get("current_weather", {})
        temperature = current_weather.get("temperature", "未知")
        windspeed = current_weather.get("windspeed", "未知")
        winddirection = current_weather.get("winddirection", "未知")
        weathercode = current_weather.get("weathercode", "未知")
        
        # 天气代码映射
        weather_codes = {
            0: "晴朗",
            1: "主要为晴天",
            2: "部分多云",
            3: "多云",
            45: "有雾",
            48: "霾",
            51: "小雨",
            53: "中雨",
            55: "大雨",
            56: "冻雨",
            57: "冻雨",
            61: "小雨",
            63: "中雨",
            65: "大雨",
            66: "冻雨",
            67: "冻雨",
            71: "小雪",
            73: "中雪",
            75: "大雪",
            77: "雪花",
            80: "阵雨",
            81: "中雨",
            82: "大雨",
            85: "小雪",
            86: "大雪",
            95: "雷暴",
            96: "雷暴",
            99: "雷暴"
        }
        
        weather_desc = weather_codes.get(weathercode, "未知")
        
        # 格式化输出
        result = city + "的天气情况：\n"
        result += "天气：" + weather_desc + "\n"
        result += "温度：" + str(temperature) + "°C\n"
        result += "风速：" + str(windspeed) + " km/h\n"
        result += "风向：" + str(winddirection) + "°\n"
        result += "更新时间：" + datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        return result
        
    except Exception as e:
        return "获取天气信息失败：" + str(e)

def main():
    """主函数"""
    if len(sys.argv) < 2:
        print("请提供城市名称")
        sys.exit(1)
    
    city = " ".join(sys.argv[1:])
    weather_info = get_weather(city)
    print(weather_info)

if __name__ == "__main__":
    main()
