#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import sys
import subprocess
import os

def install_package(package_name):
    """安装缺失的依赖包"""
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", package_name, "--break-system-packages"])
        print("已成功安装依赖包: " + package_name)
    except Exception as e:
        print("安装依赖包失败: " + str(e))
        return False
    return True

def check_dependencies():
    """检查并安装必要的依赖"""
    required_packages = ["reportlab"]
    
    for package in required_packages:
        try:
            if package == "reportlab":
                import reportlab
        except ImportError:
            print("正在安装缺失的依赖: " + package)
            if not install_package(package):
                return False
    return True

def generate_pdf(content, output_filename="output.pdf", title="文档"):
    """生成PDF文件"""
    try:
        from reportlab.lib.pagesizes import A4
        from reportlab.pdfgen import canvas
        from reportlab.lib.units import cm
        from reportlab.pdfbase import pdfmetrics
        from reportlab.pdfbase.ttfonts import TTFont
        from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
        from reportlab.platypus import Paragraph, SimpleDocTemplate, Spacer, PageBreak
        from reportlab.lib.enums import TA_LEFT, TA_CENTER
        from reportlab.lib.colors import black, navy
        
        # 注册中文字体（解决中文显示为黑块的问题）
        # 使用系统自带的中文字体或指定字体文件路径
        font_paths = [
            "/System/Library/Fonts/PingFang.ttc",  # macOS
            "/System/Library/Fonts/STHeiti Light.ttc",  # macOS
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf",  # Linux
            "C:/Windows/Fonts/simhei.ttf",  # Windows 黑体
            "C:/Windows/Fonts/simsun.ttc",  # Windows 宋体
            "C:/Windows/Fonts/msyh.ttc",  # Windows 微软雅黑
        ]
        
        chinese_font_registered = False
        for font_path in font_paths:
            if os.path.exists(font_path):
                try:
                    # 注册字体
                    pdfmetrics.registerFont(TTFont("ChineseFont", font_path))
                    chinese_font_registered = True
                    print("已注册中文字体: " + font_path)
                    break
                except Exception as e:
                    print("注册字体失败 " + font_path + ": " + str(e))
                    continue
        
        # 如果找不到系统字体，尝试使用默认字体或提示用户
        if not chinese_font_registered:
            print("警告: 未找到系统中文字体，尝试使用默认字体")
            try:
                # 尝试使用reportlab自带的字体
                from reportlab.pdfbase.cidfonts import UnicodeCIDFont
                pdfmetrics.registerFont(UnicodeCIDFont("STSong-Light"))
                chinese_font_registered = True
                print("已使用默认中文字体: STSong-Light")
            except Exception as e:
                print("使用默认中文字体失败: " + str(e))
                return False, "无法找到合适的中文字体，请确保系统安装了中文字体"
        
        # 创建PDF文档
        doc = SimpleDocTemplate(
            output_filename,
            pagesize=A4,
            rightMargin=2*cm,
            leftMargin=2*cm,
            topMargin=2*cm,
            bottomMargin=2*cm
        )
        
        # 准备内容
        story = []
        styles = getSampleStyleSheet()
        
        # 自定义标题样式（使用中文字体）
        title_style = ParagraphStyle(
            name='CustomTitle',
            parent=styles['Heading1'],
            fontSize=24,
            textColor=navy,
            spaceAfter=30,
            alignment=TA_CENTER
        )
        if chinese_font_registered:
            title_style.fontName = "ChineseFont"
        
        # 自定义副标题样式（使用中文字体）
        subtitle_style = ParagraphStyle(
            name='CustomSubtitle',
            parent=styles['Heading2'],
            fontSize=16,
            textColor=navy,
            spaceAfter=20,
            spaceBefore=20
        )
        if chinese_font_registered:
            subtitle_style.fontName = "ChineseFont"
        
        # 自定义正文样式（使用中文字体）
        body_style = ParagraphStyle(
            name='CustomBody',
            parent=styles['Normal'],
            fontSize=12,
            textColor=black,
            spaceAfter=12,
            alignment=TA_LEFT
        )
        if chinese_font_registered:
            body_style.fontName = "ChineseFont"
        
        # 添加标题
        title_para = Paragraph(title, title_style)
        story.append(title_para)
        
        story.append(Spacer(1, 20))
        
        # 添加内容
        content_lines = content.split('\n')
        for line in content_lines:
            if line.strip():
                # 根据内容格式自动应用样式
                if line.startswith("特点：") or line.startswith("功能：") or line.startswith("简介："):
                    para = Paragraph(line, subtitle_style)
                elif line.startswith("一、") or line.startswith("二、") or line.startswith("三、") or line.startswith("四、") or line.startswith("五、"):
                    para = Paragraph(line, subtitle_style)
                elif line.startswith("1、") or line.startswith("2、") or line.startswith("3、") or line.startswith("4、") or line.startswith("5、"):
                    para = Paragraph("&nbsp;&nbsp;&nbsp;&nbsp;" + line, body_style)
                elif line.startswith("• ") or line.startswith("- ") or line.startswith("* "):
                    para = Paragraph("&nbsp;&nbsp;&nbsp;&nbsp;" + line, body_style)
                else:
                    para = Paragraph(line, body_style)
                story.append(para)
                story.append(Spacer(1, 8))
        
        # 生成PDF
        doc.build(story)
        
        return True, output_filename
        
    except Exception as e:
        return False, "生成PDF时出错: " + str(e)

def main(args):
    if args and args[0] == "test":
        print("test ok")
        return
    
    # 检查依赖
    if not check_dependencies():
        print("依赖检查失败，无法继续执行")
        return
    
    # 从命令行参数获取内容
    if len(args) >= 1:
        # 第一个参数是文档内容
        content = args[0]
        
        # 第二个参数是输出文件名（可选）
        if len(args) >= 2:
            output_filename = args[1]
            # 确保文件名以.pdf结尾
            if not output_filename.lower().endswith('.pdf'):
                output_filename = output_filename + '.pdf'
        else:
            # 如果没有提供输出文件名，使用默认文件名
            output_filename = "document.pdf"
        
        # 第三个参数是标题（可选）
        if len(args) >= 3:
            title = args[2]
        else:
            # 如果没有提供标题，使用默认标题
            title = "文档"
        
        # 生成PDF
        success, result = generate_pdf(content, output_filename, title)
        
        if success:
            print("PDF文件已成功生成: " + result)
            print("文件保存在当前目录: " + os.path.abspath(result))
        else:
            print("生成PDF失败: " + result)
    else:
        print("使用方法: python script.py <文档内容> [输出文件名] [标题]")
        print("示例: python script.py '这是文档内容' mydoc.pdf '我的文档'")
        print("测试: python script.py test")

if __name__ == "__main__":
    main(sys.argv[1:])