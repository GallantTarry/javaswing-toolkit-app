# -*- coding: utf-8 -*-
"""
[007] PDF 批量拆分与提取指定页工具
场景：精准剥离PDF指定页（支持"1,3,5-10"格式），或将几百页长文档全部拆解为单页独立文件。
核心库：pypdf / tkinter
"""
import os
import sys
import subprocess
import tkinter as tk
from tkinter import filedialog, messagebox

# ==========================================
# 🚀 模块 1：全自动环境嗅探与安装
# ==========================================
def auto_install_env():
    """检测并自动安装脚本所需的第三方库"""
    dependencies = {
        "pypdf": "pypdf"  # PyPDF2 的最新官方维护版本
    }
    
    for pkg_name, import_name in dependencies.items():
        try:
            __import__(import_name)
        except ImportError:
            print(f"📦 首次运行，正在为你全自动安装必备环境: {pkg_name} ...")
            try:
                subprocess.check_call([
                    sys.executable, "-m", "pip", "install", pkg_name, 
                    "-i", "https://pypi.tuna.tsinghua.edu.cn/simple"
                ])
                print(f"✅ {pkg_name} 安装成功！")
            except Exception as e:
                print(f"❌ 安装 {pkg_name} 失败，请手动检查网络。报错: {e}")
                sys.exit(1)

# 启动即自检
auto_install_env()

from pypdf import PdfReader, PdfWriter

# ==========================================
# 🚀 模块 2：核心业务逻辑与解析器
# ==========================================
def parse_page_range(range_str, max_pages):
    """
    智能解析页码输入格式，例如："1, 3, 5-10"
    自动过滤越界页码，并去重排序
    """
    pages = set()
    parts = range_str.split(',')
    
    for part in parts:
        part = part.strip()
        if not part:
            continue
            
        if '-' in part:
            # 处理范围格式，例如 "5-10"
            sub_parts = part.split('-', 1)
            start = int(sub_parts[0].strip())
            end = int(sub_parts[1].strip())
            
            # 确保从小到大
            if start > end:
                start, end = end, start
                
            for p in range(start, end + 1):
                if 1 <= p <= max_pages:
                    pages.add(p)
        else:
            # 处理单页格式，例如 "3"
            p = int(part)
            if 1 <= p <= max_pages:
                pages.add(p)
                
    return sorted(list(pages))

def select_pdf():
    """选择待处理的 PDF 文件"""
    file_path = filedialog.askopenfilename(
        title="请选择需要拆分或提取的 PDF 文件",
        filetypes=[("PDF 文档", "*.pdf")]
    )
    if file_path:
        entry_pdf.config(state=tk.NORMAL)
        entry_pdf.delete(0, tk.END)
        entry_pdf.insert(0, file_path)
        entry_pdf.config(state=tk.DISABLED)
        
        # 顺便读取一下总页数展示给用户
        try:
            reader = PdfReader(file_path)
            lbl_pages_info.config(text=f"已加载: 共 {len(reader.pages)} 页")
        except Exception:
            lbl_pages_info.config(text="已加载: 页数未知")

def toggle_mode():
    """根据选择的模式，启用或禁用页码输入框"""
    if var_mode.get() == 1:
        entry_pages.config(state=tk.NORMAL)
    else:
        entry_pages.config(state=tk.DISABLED)

def start_extraction():
    """核心提取与拆解逻辑"""
    pdf_path = entry_pdf.get()
    mode = var_mode.get()
    pages_input = entry_pages.get().strip()
    
    if not pdf_path:
        messagebox.showwarning("操作提示", "少侠，请先选择需要处理的 PDF 文件！")
        return

    # 锁定按钮
    btn_start.config(text="正在全速施法中...", state=tk.DISABLED)
    root.update()

    try:
        reader = PdfReader(pdf_path)
        total_pages = len(reader.pages)
        
        base_dir = os.path.dirname(pdf_path)
        base_name = os.path.splitext(os.path.basename(pdf_path))[0]

        # ================= 模式 1：精准提取指定页 =================
        if mode == 1:
            if not pages_input:
                messagebox.showwarning("操作提示", "请输入需要提取的页码范围！")
                reset_button()
                return
                
            try:
                target_pages = parse_page_range(pages_input, total_pages)
            except ValueError:
                messagebox.showerror("格式错误", "页码格式不正确！\n请使用数字、逗号和连字符，例如：1,3,5-10")
                reset_button()
                return
                
            if not target_pages:
                messagebox.showwarning("范围无效", f"提取失败！原文档共 {total_pages} 页，您输入的页码均超出了范围。")
                reset_button()
                return
                
            writer = PdfWriter()
            # 索引是从 0 开始的，所以真实页码需减 1
            for p in target_pages:
                writer.add_page(reader.pages[p - 1])
                
            output_file = os.path.join(base_dir, f"{base_name}_提取结果.pdf")
            with open(output_file, "wb") as f:
                writer.write(f)
                
            msg = f"✅ 提取成功！\n\n共为您提取了 {len(target_pages)} 页内容并合成了新文档。\n📁 文件保存至: {output_file}"
            messagebox.showinfo("处理完毕", msg)

        # ================= 模式 2：全量拆解为单页 =================
        elif mode == 2:
            output_dir = os.path.join(base_dir, f"{base_name}_单页拆解结果")
            if not os.path.exists(output_dir):
                os.makedirs(output_dir)
                
            success_count = 0
            for i in range(total_pages):
                writer = PdfWriter()
                writer.add_page(reader.pages[i])
                
                # 为了保持排序，文件名会根据总页数自动补零，例如: 第001页
                zero_padding = len(str(total_pages))
                page_num_str = str(i + 1).zfill(zero_padding)
                
                output_file = os.path.join(output_dir, f"{base_name}_第{page_num_str}页.pdf")
                with open(output_file, "wb") as f:
                    writer.write(f)
                success_count += 1
                
            msg = f"✅ 全量拆解成功！\n\n原文档共 {total_pages} 页，已全部化为 {success_count} 个独立的单页文件。\n📁 专属存放文件夹: {output_dir}"
            messagebox.showinfo("处理完毕", msg)

    except Exception as e:
        messagebox.showerror("运行崩溃", f"处理过程中发生意外，请检查文件是否被加密。\n\n报错详情: {e}")
    finally:
        reset_button()

def reset_button():
    btn_start.config(text="🚀 开始执行拆分与提取", state=tk.NORMAL)


# ==========================================
# 🚀 模块 3：可视化操作面板 构建
# ==========================================
root = tk.Tk()
root.title("[013] PDF 批量拆分与提取神器")
root.geometry("560x320")
root.resizable(False, False)
root.attributes('-topmost', True)

padx, pady = 15, 10

# 1. 文件选择
tk.Label(root, text="待处理的长 PDF:").grid(row=0, column=0, padx=padx, pady=(20, pady), sticky="e")
entry_pdf = tk.Entry(root, width=38, state=tk.DISABLED)
entry_pdf.grid(row=0, column=1, pady=(20, pady))
tk.Button(root, text="浏览...", command=select_pdf).grid(row=0, column=2, padx=10, pady=(20, pady))

# 信息展示标签
lbl_pages_info = tk.Label(root, text="未选择文件", fg="#009688", font=("Arial", 9, "bold"))
lbl_pages_info.grid(row=1, column=1, sticky="w", pady=(0, 10))

# 2. 模式选择 (单选框)
tk.Label(root, text="核心法宝模式:").grid(row=2, column=0, padx=padx, pady=pady, sticky="e")
frame_mode = tk.Frame(root)
frame_mode.grid(row=2, column=1, columnspan=2, sticky="w", pady=pady)

var_mode = tk.IntVar(value=1)
tk.Radiobutton(frame_mode, text="模式A: 精准提取指定页码合并", variable=var_mode, value=1, command=toggle_mode).pack(anchor="w")
tk.Radiobutton(frame_mode, text="模式B: 将每一页拆解为独立文件", variable=var_mode, value=2, command=toggle_mode).pack(anchor="w", pady=(5,0))

# 3. 页码输入区 (关联模式A)
tk.Label(root, text="提取页码规则:").grid(row=3, column=0, padx=padx, pady=pady, sticky="e")
entry_pages = tk.Entry(root, width=38)
entry_pages.grid(row=3, column=1, pady=pady)
entry_pages.insert(0, "1, 3, 5-10") # 默认占位符提示
tk.Label(root, text="(仅模式A有效)", fg="gray", font=("Arial", 9)).grid(row=3, column=2, sticky="w")

# 4. 执行按钮
btn_start = tk.Button(root, text="🚀 开始执行拆分与提取", bg="#2196F3", fg="white", font=("Arial", 11, "bold"), width=22, command=start_extraction)
btn_start.grid(row=4, column=0, columnspan=3, pady=(15, 10))

if __name__ == "__main__":
    root.mainloop()